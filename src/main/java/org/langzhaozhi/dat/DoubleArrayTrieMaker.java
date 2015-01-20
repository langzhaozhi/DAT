package org.langzhaozhi.dat;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Map;

import org.langzhaozhi.dat.DoubleArrayTrie.DoubleArrayTrieNode;
import org.langzhaozhi.dat.Trie.TrieNode;
import org.langzhaozhi.util.StringPair;

/**
 * DAT生成器：构建 DoubleArrayTrie
 */
public final class DoubleArrayTrieMaker {
    public static <T> DoubleArrayTrie<T> makeDoubleArrayTrie(StringPair<T> [] aValueArray) {
        //先排字典序才能后续处理：采用并行排序，当数据量大就真的显示出并行排序的威力了，数据量小的话Arrays.parallelSort自动按照普通排序做
        Arrays.parallelSort( aValueArray );

        MakeContext<T> context = new MakeContext<T>( aValueArray );
        ProccessingNode<T> rootProcessingNode = context.mDatArray[ 0 ];
        //以bfs遍历方式生成Trie
        LinkedList<ProccessingNode<T>> queueFetch = new LinkedList<ProccessingNode<T>>();
        LinkedList<ProccessingNode<T>> queueInsert = new LinkedList<ProccessingNode<T>>();
        //fetch 从虚根作为父节点开始构建 Trie 的过程
        queueFetch.add( rootProcessingNode );
        while (!queueFetch.isEmpty()) {
            ProccessingNode<T> nextParentNode = queueFetch.removeFirst();
            DoubleArrayTrieMaker.fetch( context, queueFetch, nextParentNode );
            if (nextParentNode.mChildrenNodes != null) {
                queueInsert.add( nextParentNode );
            }
        }
        @SuppressWarnings("unchecked")
        ProccessingNode<T> [] parentNodes = queueInsert.toArray( new ProccessingNode [ queueInsert.size() ] );
        return DoubleArrayTrieMaker.construct( context, parentNodes );
    }

    public static <T> DoubleArrayTrieAhoCorasick<T> makeAhoCorasick(StringPair<T> [] aValueArray) {
        return DoubleArrayTrieMaker.makeDoubleArrayTrie( aValueArray ).asAhoCorasick();
    }

    public static <T> DoubleArrayTrieMap<T> makeMap(StringPair<T> [] aValueArray) {
        return DoubleArrayTrieMaker.makeDoubleArrayTrie( aValueArray ).asMap();
    }

    public static <T> DoubleArrayTrieMap<T> convert(Map<String, T> aLowSpeedMap) {
        @SuppressWarnings("unchecked")
        StringPair<T> [] pairs = aLowSpeedMap.entrySet().stream().map( (aNextEntry) -> new StringPair<T>( aNextEntry.getKey(), aNextEntry.getValue() ) ).toArray( StringPair []::new );
        return DoubleArrayTrieMaker.makeMap( pairs );
    }

    /**
     * 把 Trie<T> 树此时刻的镜像数据转换成DAT，一般在Trie树构建完毕后转成DAT以提供急速的并发访问
     * @param aTrie 一棵Trie树
     * @return DAT
     */
    public static <T> DoubleArrayTrie<T> convert(Trie<T> aTrie) {
        MakeContext<T> context = new MakeContext<T>( 1 );
        //先对Trie树遍历一遍把TrieNode转换成需要额外信息(base,check,depth等)的ProcessingNode
        LinkedList<ProccessingNode<T>> queueInsert = new LinkedList<ProccessingNode<T>>();
        DoubleArrayTrieMaker.tryConvertTrieNode( aTrie.mRootTrieNode, context.mDatArray[ 0 ], queueInsert );

        @SuppressWarnings("unchecked")
        ProccessingNode<T> [] parentNodes = queueInsert.toArray( new ProccessingNode [ queueInsert.size() ] );
        return DoubleArrayTrieMaker.construct( context, parentNodes );
    }

    /*
    public static <T> DoubleArrayTrie<T> deserializeDoubleArrayTrieFromFile(File aInputFile, ValueDeserializer<T> aValueDeserializer) throws IOException {
        try (BufferedReader datReader = new BufferedReader( new InputStreamReader( new BufferedInputStream( new FileInputStream( aInputFile ), 1024 << 8 ), "UTF-8" ) )) {
            String firstLine = datReader.readLine();
            if (!firstLine.startsWith( "#BeginOutputDAT(" ) || !firstLine.endsWith( ")" )) {//基本的持久化称性检查
                throw new Error( "搞错文件喽，走错女厕所喽:" + aInputFile.getAbsolutePath() );
            }
            int datArrayLength = Integer.parseInt( firstLine.substring( firstLine.indexOf( '(' ) + 1, firstLine.lastIndexOf( ')' ) ) );
            @SuppressWarnings("unchecked")
            DoubleArrayTrieNode<T> [] datArray = new DoubleArrayTrieNode [ datArrayLength ];
            char [] chars = new char [ datArrayLength ];//用于校验看文件是否一致
            for (String nextLine = datReader.readLine(); nextLine != null; nextLine = datReader.readLine()) {
                if (nextLine.charAt( 0 ) == '#') {
                    if (!nextLine.equals( "#EndOutputDAT" ) || (nextLine = datReader.readLine()) != null) {//最后一行，也对称性检查下
                        throw new Error( "End Format Error For DAT File:" + aInputFile.getAbsolutePath() );
                    }
                }
                else {
                    String [] parts = nextLine.split( "\t" );
                    int datIndex = Integer.parseInt( parts[ 0 ], 16 );
                    chars[ datIndex ] = parts[ 1 ].charAt( 0 );
                    int base = Integer.parseInt( parts[ 2 ] );
                    int check = Integer.parseInt( parts[ 3 ] );
                    String valuePart = nextLine.substring( parts[ 0 ].length() + parts[ 1 ].length() + parts[ 2 ].length() + parts[ 3 ].length() + 4 );//因为T value内部也可能用\t分割，必须传递完整
                    T value = valuePart.equals( "[]" ) ? null : aValueDeserializer.deserialize( valuePart.substring( 2, valuePart.length() - 2 ) );
                    datArray[ datIndex ] = new DoubleArrayTrieNode<T>( base, check, value );
                }
            }
            if (datArray[ datArrayLength - 1 ] == null) {//最后一个dat数组节点绝对不可能为null否则就是不一致了
                throw new Error( "End Check Error: " + aInputFile.getAbsolutePath() );
            }
            //再根据chars把父子关系的base和check校验一盘：childCheck == parentIndex && childIndex == parentBase + childChar 必须成立
            for (int i = 1; i < datArrayLength; ++i) {//从1开始，虚根不用
                DoubleArrayTrieNode<T> childDatNode = datArray[ i ];
                if (childDatNode != null) {
                    if (datArray[ childDatNode.mCheck ].mBase + chars[ i ] != i) {
                        throw new Error( "Sequence Check Error: " + aInputFile.getAbsolutePath() + ":[" + i + "," + chars[ i ] + "]" );
                    }
                }
            }
            return new DoubleArrayTrie<T>( datArray );
        }
    }
    */

    public static <T> DoubleArrayTrie<T> deserializeDoubleArrayTrieFromFile(File aInputFile, ValueDeserializer<T> aValueDeserializer) throws IOException {
        try (FileInputStream fis = new FileInputStream( aInputFile )) {
            FileChannel fc = fis.getChannel();
            ByteBuffer fileBuffer = fis.getChannel().map( MapMode.READ_ONLY, 0, fc.size() );
            if (fileBuffer.getInt() != ByteBuffer.wrap( "#DAT".getBytes( "UTF-8" ) ).getInt()) {//check simple magic
                throw new Error( "搞错文件喽，走错女厕所喽:" + aInputFile.getAbsolutePath() );
            }
            int datArrayLength = fileBuffer.getInt();
            @SuppressWarnings("unchecked")
            DoubleArrayTrieNode<T> [] datArray = new DoubleArrayTrieNode [ datArrayLength ];
            char [] chars = new char [ datArrayLength ];//用于校验看文件是否一致
            int lastIndex = datArrayLength - 1;
            while (true) {
                int datIndex = fileBuffer.getInt();
                chars[ datIndex ] = fileBuffer.getChar();
                int base = fileBuffer.getInt();
                int check = fileBuffer.getInt();
                T value = null;
                if (fileBuffer.get() != 0) {
                    //dataBuffer
                    int valueByteCount = fileBuffer.getInt();
                    int beforePos = fileBuffer.position();
                    int beforeLimit = fileBuffer.limit();
                    int afterPos = beforePos + valueByteCount;
                    fileBuffer.limit( afterPos );
                    value = aValueDeserializer.deserialize( fileBuffer );
                    fileBuffer.limit( beforeLimit ).position( afterPos );
                }
                datArray[ datIndex ] = new DoubleArrayTrieNode<T>( base, check, value );
                if (datIndex == lastIndex) {
                    break;
                }
            }
            //再根据chars把父子关系的base和check校验一盘：childCheck == parentIndex && childIndex == parentBase + childChar 必须成立
            for (int i = 1; i < datArrayLength; ++i) {//从1开始，虚根不用
                DoubleArrayTrieNode<T> childDatNode = datArray[ i ];
                if (childDatNode != null) {
                    if (datArray[ childDatNode.mCheck ].mBase + chars[ i ] != i) {
                        throw new Error( "Sequence Check Error: " + aInputFile.getAbsolutePath() + ":[" + i + "," + chars[ i ] + "]" );
                    }
                }
            }
            return new DoubleArrayTrie<T>( datArray );
        }
    }

    /*
    public static <T> void serializeDoubleArrayTrieFromFile(DoubleArrayTrie<T> aDAT, File aOutputFile, ValueSerializer<T> aValueSerializer) throws IOException {
        try (PrintWriter datWriter = new PrintWriter( new OutputStreamWriter( new BufferedOutputStream( new FileOutputStream( aOutputFile ), 1024 << 6 ), "UTF-8" ) )) {
            DoubleArrayTrieNode<T> [] datArray = aDAT.mDatArray;
            int invalidBase = datArray[ 0 ].mBase - 1;//虚根的mBase一定是最小的base值，则把其他叶子节点的base值序化成小1不用Integer.MIN_VALUE
            datWriter.println( "#BeginOutputDAT(" + datArray.length + ")" );//datArrayLength
            for (int i = 0, isize = datArray.length; i < isize; ++i) {
                DoubleArrayTrieNode<T> n = datArray[ i ];
                if (n != null) {
                    datWriter.append( Integer.toHexString( i ).toUpperCase() ).append( '\t' ).append( n.getChar( datArray, i ) ).append( '\t' ).append( Integer.toString( n.mBase == Integer.MIN_VALUE ? invalidBase : n.mBase ) ).append( '\t' ).append( Integer.toString( n.mCheck ) ).append( '\t' ).append( '[' );
                    if (n.mValue != null) {
                        datWriter.append( '[' ).append( aValueSerializer.serialize( n.mValue ) ).append( ']' );//内层数据也用[]
                    }
                    datWriter.append( ']' ).println();
                }
            }
            datWriter.println( "#EndOutputDAT" );
        }
    }
    */

    public static <T> void serializeDoubleArrayTrieFromFile(DoubleArrayTrie<T> aDAT, File aOutputFile, ValueSerializer<T> aValueSerializer) throws IOException {
        try (DataOutputStream datWriter = new DataOutputStream( new BufferedOutputStream( new FileOutputStream( aOutputFile ), 1024 << 6 ) )) {
            DoubleArrayTrieNode<T> [] datArray = aDAT.mDatArray;
            //int invalidBase = datArray[ 0 ].mBase - 1;//虚根的mBase一定是最小的base值，则把其他叶子节点的base值序化成小1不用Integer.MIN_VALUE
            datWriter.write( "#DAT".getBytes() );//simple magic
            datWriter.writeInt( datArray.length );//datArrayLength
            for (int i = 0, isize = datArray.length; i < isize; ++i) {
                DoubleArrayTrieNode<T> n = datArray[ i ];
                if (n != null) {
                    datWriter.writeInt( i );
                    datWriter.writeChar( n.getChar( datArray, i ) );
                    datWriter.writeInt( n.mBase );
                    datWriter.writeInt( n.mCheck );
                    if (n.mValue != null) {
                        datWriter.write( 1 );
                        byte [] valueBytes = aValueSerializer.serialize( n.mValue );
                        datWriter.writeInt( valueBytes.length );
                        datWriter.write( valueBytes );
                    }
                    else {
                        datWriter.write( 0 );
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> boolean tryConvertTrieNode(TrieNode<T> aTrieNode, ProccessingNode<T> aDatNode, LinkedList<ProccessingNode<T>> aQueueInsert) {
        int childCount = aTrieNode.mChildCount;
        if (childCount > 0) {
            //由于深度优先递归调用不能用共享的MakeContext.mCacheChildNodeList
            ArrayList<ProccessingNode<T>> childrenNodeList = new ArrayList<ProccessingNode<T>>( childCount );
            int childDepth = aDatNode.mDepth + 1;
            for (int i = 0; i < childCount; ++i) {//Trie树中可能有很多无效的节点，需要排除，例如叶子Trie节点不是数据节点之类
                TrieNode<T> childTrieNode = aTrieNode.mChildrenNodes[ i ];
                ProccessingNode<T> childDatNode = new ProccessingNode<T>( childTrieNode.mChar, childDepth, 0 );
                if (DoubleArrayTrieMaker.tryConvertTrieNode( childTrieNode, childDatNode, aQueueInsert )) {
                    childrenNodeList.add( childDatNode );
                }
            }
            int validChildCount = childrenNodeList.size();
            if (validChildCount > 0) {
                aDatNode.mChildrenNodes = childrenNodeList.toArray( new ProccessingNode [ validChildCount ] );
                aQueueInsert.add( aDatNode );//只插入儿子节点非空的父亲节点
            }
        }
        if (aTrieNode.mValue != null) {
            aDatNode.mValue = aTrieNode.mValue;
        }
        //如果trieNode是数据节点则有效，如果存在一个儿子trieNode有效那么该父节点有效
        return aDatNode.mValue != null || aDatNode.mChildrenNodes != null;
    }

    private static <T> void fetch(MakeContext<T> aContext, LinkedList<ProccessingNode<T>> aQueue, ProccessingNode<T> aParentNode) {
        //根据字典序构造下层Trie结构, childrenNodeList 就是 aParentNode 的儿子
        StringPair<T> [] valueArray = aContext.mValueArray;
        ArrayList<ProccessingNode<T>> childrenNodeList = null;
        for (int i = aParentNode.mLeft, size = aParentNode.mRight, preChar = -1, parentDepth = aParentNode.mDepth, childDepth = parentDepth + 1; i < size; ++i) {
            StringPair<T> nextValue = valueArray[ i ];
            int keyCharCount = nextValue.mKey.length();
            if (keyCharCount > parentDepth) {
                char childChar = nextValue.mKey.charAt( parentDepth );
                if (childChar != preChar) {
                    ProccessingNode<T> nextChildNode = new ProccessingNode<T>( childChar, childDepth, i );
                    if (childrenNodeList == null) {
                        childrenNodeList = aContext.mCacheChildNodeList;
                    }
                    childrenNodeList.add( nextChildNode );
                    if (keyCharCount == childDepth) {
                        //这个是数据节点，只要mValue非空的就是数据节点，注意的是数据节点不一定就是叶子节点
                        nextChildNode.mValue = nextValue.mValue;
                    }
                    preChar = childChar;
                    //加入bfs遍历队列
                    aQueue.add( nextChildNode );
                }
            }
            else {
                //与parent重复的数据或者空串,就用父节点覆盖，也就是所有相同的Key只有同一个节点，注意如果是空串的话就是虚根节点
                aParentNode.mValue = nextValue.mValue;
            }
        }
        if (childrenNodeList != null) {
            //构建DAT插入过程,为加快处理速度把ArrayList直接转换成数组来操纵用，是因为insert过程要反复对childrenNodes迭代,避免频繁的get(i)调用
            @SuppressWarnings("unchecked")
            ProccessingNode<T> [] childrenNodes = childrenNodeList.toArray( new ProccessingNode [ childrenNodeList.size() ] );
            int lastChildIndex = childrenNodes.length - 1;
            for (int i = 0; i < lastChildIndex; ++i) {
                //标示各个儿子的mRight为下一个儿子的mLeft
                childrenNodes[ i ].mRight = childrenNodes[ i + 1 ].mLeft;
            }
            //设置最后一个儿子的mRight为父亲的mRight
            childrenNodes[ lastChildIndex ].mRight = aParentNode.mRight;
            //记录下父子关系
            aParentNode.mChildrenNodes = childrenNodes;
            //清理aContext.mCacheChildNodeList以备下次继续用
            childrenNodeList.clear();
        }
    }

    private static <T> void insert(MakeContext<T> aContext, ProccessingNode<T> aParentNode) {
        ProccessingNode<T> [] childrenNodes = aParentNode.mChildrenNodes;
        int firstChildChar = childrenNodes[ 0 ].mChar;
        int lastChildChar = childrenNodes[ childrenNodes.length - 1 ].mChar;
        //使得parentNode的mBase设置成恰好从dat数组的下标从 aNextFirstEmptyIndex 开始检查冲突,可能是负数的哦
        int parentBase = aContext.getNextFirstEmptyIndex() - firstChildChar;
        ProccessingNode<T> [] datArray = aContext.ensureDatArrayLength( parentBase + lastChildChar );
        for (int i = 0; i < childrenNodes.length; ++i) {
            ProccessingNode<T> nextChildNode = childrenNodes[ i ];
            if (datArray[ parentBase + nextChildNode.mChar ] != null) {
                //解决冲突:寻找aChildrenNodes.length个空闲空间使得 parentBase 满足所有的长度为空的空间
                ++parentBase;
                datArray = aContext.ensureDatArrayLength( parentBase + lastChildChar );
                //又要从0开始检查
                i = -1;
            }
        }
        //把消除冲突后的parentBase记录下来
        aParentNode.mBase = parentBase;
        //将解决冲突后的结果存入dat 数组
        for (int i = 0; i < childrenNodes.length; ++i) {
            ProccessingNode<T> nextChildNode = childrenNodes[ i ];
            nextChildNode.mIndex = parentBase + nextChildNode.mChar;
            //不用设置mCheck,因为aParentNode可能还没有插入dat
            //nextChildNode.mCheck = parentIndex;
            datArray[ nextChildNode.mIndex ] = nextChildNode;
        }
    }

    private static <T> DoubleArrayTrie<T> construct(MakeContext<T> aContext, ProccessingNode<T> [] aParentNodes) {
        //还是按照层的顺序，以儿子数目多少优先插入，这种测试对比目前这种方法使得dat数组长度最小，内存占用自然也小，以后再考虑其他组合方式
        Arrays.parallelSort( aParentNodes, (aOne, aTwo) -> {
            int childCountDelta = aTwo.mChildrenNodes.length - aOne.mChildrenNodes.length;//由大到小
            if (childCountDelta != 0) {//优先儿子个数排序，测试发现这种方式dat数组的长度压缩率很高，其他方式都不太行
                return childCountDelta;
            }
            else {
                int depthDelta = aOne.mDepth - aTwo.mDepth;//由小到大
                return depthDelta != 0 ? depthDelta : (aTwo.mChar - aOne.mChar);
            }
        } );
        //排完序后依次插入
        for (ProccessingNode<T> nextParentNode : aParentNodes) {
            DoubleArrayTrieMaker.insert( aContext, nextParentNode );
        }
        //此时所有节点插入完毕再构造mCheck关系
        for (ProccessingNode<T> nextParentNode : aParentNodes) {
            int parentIndex = nextParentNode.mIndex;
            ProccessingNode<T> [] childrenNodes = nextParentNode.mChildrenNodes;
            for (ProccessingNode<T> childNode : childrenNodes) {
                childNode.mCheck = parentIndex;
            }
        }
        return aContext.toDoubleArrayTrie();
    }

    private static final class ProccessingNode<T> {
        //最终需要的结果mBase,mCheck和mValue
        private int mBase = Integer.MIN_VALUE;//叶子节点的mBase用Integer.MIN_VALUE
        private int mCheck;
        private T mValue;
        //下面是构造临时信息
        private char mChar;
        private int mDepth;
        private int mIndex = -1;//初始化成无效的-1
        private int mLeft;
        private int mRight;
        private ProccessingNode<T> [] mChildrenNodes;

        ProccessingNode(int aTotalCount) {
            //虚根
            this.mChar = '\0';
            this.mDepth = 0;
            this.mIndex = 0;
            this.mLeft = 0;
            this.mRight = aTotalCount;
        }

        ProccessingNode(char aChar, int aDepth, int aLeft) {
            //子节点
            this.mChar = aChar;
            this.mDepth = aDepth;
            this.mLeft = aLeft;
        }
    }

    /**
     * 构建DAT的上下文对象,用于保存构建过程中的数据
     */
    private static final class MakeContext<T> {
        StringPair<T> [] mValueArray;
        ProccessingNode<T> [] mDatArray;
        //mNextFirstEmptyIndex表示dat数组第一个空位的位置,后面的dat构建过程就是从插入下标1的位置开始,因为下标0是虚根的嘛
        int mNextFirstEmptyIndex = 1;
        ArrayList<ProccessingNode<T>> mCacheChildNodeList;

        MakeContext(StringPair<T> [] aValueArray) {
            this( aValueArray.length );
            this.mValueArray = aValueArray;
            //避免每次创建用途的cache
            this.mCacheChildNodeList = new ArrayList<ProccessingNode<T>>( aValueArray.length );
        }

        MakeContext(int aArrayCount) {
            @SuppressWarnings("unchecked")
            ProccessingNode<T> [] datArray = new ProccessingNode [ 1 + aArrayCount << 1 ];
            //创建一个虚根节点,对应dat数组的下标0
            ProccessingNode<T> proccessingNodeRoot = new ProccessingNode<T>( aArrayCount );
            datArray[ 0 ] = proccessingNodeRoot;
            this.mDatArray = datArray;
        }

        int getNextFirstEmptyIndex() {
            ProccessingNode<T> [] datArray = this.mDatArray;
            int nextFirstEmptyIndex = this.mNextFirstEmptyIndex;
            for (; nextFirstEmptyIndex < datArray.length; ++nextFirstEmptyIndex) {
                if (datArray[ nextFirstEmptyIndex ] == null) {
                    //mNextFirstEmptyIndex是dat数组中从左到右的第一个空位下标,下次mBase冲突检查就从这个aNextFirstEmptyIndex开始
                    //需要记录下来，否则每次都从1开始查找没有必要
                    this.mNextFirstEmptyIndex = nextFirstEmptyIndex;
                    break;
                }
            }
            return nextFirstEmptyIndex;
        }

        ProccessingNode<T> [] ensureDatArrayLength(int aPos) {
            ProccessingNode<T> [] datArray = this.mDatArray;
            if (datArray.length <= aPos) {
                datArray = Arrays.copyOf( datArray, aPos + 1024 );
                this.mDatArray = datArray;
            }
            return datArray;
        }

        private DoubleArrayTrie<T> toDoubleArrayTrie() {
            //构造完毕，这里的 realDatArray 才是最终的dat数组，而ProccessingNode [] datArray只是构建DAT用途，
            //构建完毕后大部分信息是冗余的，因此被丢弃，只需要保留 mBase,mCheck和 mValue即可
            //节省内存，丢弃后面所有为空的部分
            ProccessingNode<T> [] datArray = this.mDatArray;
            int realLength = datArray.length;
            for (int i = realLength - 1; i >= 0; --i) {
                if (datArray[ i ] != null) {
                    realLength = i + 1;
                    break;
                }
            }
            @SuppressWarnings("unchecked")
            DoubleArrayTrieNode<T> [] realDatArray = new DoubleArrayTrieNode [ realLength ];
            for (int i = 0; i < realLength; ++i) {
                ProccessingNode<T> pNode = datArray[ i ];
                if (pNode != null) {
                    //只保留mBase,mCheck,mValue
                    realDatArray[ i ] = new DoubleArrayTrieNode<T>( pNode.mBase, pNode.mCheck, pNode.mValue );
                    datArray[ i ] = null;//speed GC it
                }
            }
            return new DoubleArrayTrie<T>( realDatArray );
        }
    }
}
