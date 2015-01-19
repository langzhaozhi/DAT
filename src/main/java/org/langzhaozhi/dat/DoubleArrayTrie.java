package org.langzhaozhi.dat;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * <p>DAT双数组Trie结构</p>
 * <p>不变对象，意味着一旦构造就不再改变，因此可以任意多线程并发访问。</p>
 * <p>exactMatchSearch 提供极速的完全匹配搜索方式, 只有完全匹配到参数aKey的才返回结果；而commonPrefixSearch
 * 则提供极速的前缀匹配搜索方式，意味着所有匹配参数aKey前缀的结果都会返回,自然也包括了完全匹配的结果，
 * 如果匹配不到就返回一个空的List</p>
 *
 * <p>如果要进行多模式串匹配，也就是不仅仅是前缀，而是搜索包括中间、后缀等的匹配串，那么需要调用<code>asAhoCorasick()</code>
 * 转换成AhoCorasick来使用</p>
 *
 * <p>只能通过<code>DoubleArrayTrieMaker</code>的<code>makerDoubleArrayTrie(...)</code>进行构造</p>
 * @param <T>
 * @see DoubleArrayTrieAhoCorasick
 */
public final class DoubleArrayTrie<T> {
    DoubleArrayTrieNode<T> [] mDatArray;
    private DoubleArrayTrieAhoCorasick<T> mAhoCorasick;

    DoubleArrayTrie(DoubleArrayTrieNode<T> [] aDatArray) {
        //from DoubleArrayTrieMaker.makeDoubleArrayTrie()
        this.mDatArray = aDatArray;
    }

    /**
     * 极速的完全匹配搜索
     */
    public T exactMatchSearch(CharSequence aKey) {
        DoubleArrayTrieNode<T> [] datArray = this.mDatArray;
        //总是从虚根开始
        DoubleArrayTrieNode<T> searchNode = datArray[ 0 ];
        int parentCheck = searchNode.mCheck;
        for (int i = 0, keyCharLen = aKey.length(); i < keyCharLen; ++i) {
            char nextChar = aKey.charAt( i );
            int index = searchNode.mBase + nextChar;
            if (index < 0 || index >= datArray.length) {
                //由于mBase可能为负,因此这里计算出的index有可能在数组范围外
                return null;
            }
            searchNode = datArray[ index ];
            if (searchNode == null || searchNode.mCheck != parentCheck) {
                //check检查非常关键，如果check不相等，此 searchNode 肯定不是后继节点
                return null;
            }
            parentCheck = index;
        }
        return searchNode.mValue;
    }

    /**
     * 极速的前缀匹配搜索方式
     */
    public void commonPrefixSearch(CharSequence aKey, Hit<T> aHit) {
        boolean whetherContinueHit = true;
        DoubleArrayTrieNode<T> [] datArray = this.mDatArray;
        //总是从虚根开始
        DoubleArrayTrieNode<T> searchNode = datArray[ 0 ];
        int parentCheck = searchNode.mCheck;
        int keyCharLen = aKey.length();
        if (keyCharLen == 0) {
            //空串对应虚根节点
            aHit.hit( aKey, 0, 0, searchNode.mValue );
        }
        else {
            for (int i = 0; whetherContinueHit && i < keyCharLen; ++i) {
                char nextChar = aKey.charAt( i );
                int index = searchNode.mBase + nextChar;
                if (index < 0 || index >= datArray.length) {
                    //由于mBase可能为负,因此这里计算出的index有可能在数组范围外
                    break;
                }
                else {
                    searchNode = datArray[ index ];
                    if (searchNode == null || searchNode.mCheck != parentCheck) {
                        //check检查非常关键，如果check不相等，此 searchNode 肯定不是后继节点
                        break;
                    }
                    else {
                        if (searchNode.mValue != null) {
                            whetherContinueHit = aHit.hit( aKey, 0, i + 1, searchNode.mValue );
                        }
                        parentCheck = index;
                    }
                }
            }
        }
    }

    /**
     * 快速遍历所有的数据, 但不是按照Trie树结构遍历的，因为有可能儿子节点在父节点之前先hit到,如果想按照Trie
     * 树父子关系遍历，也就是父亲数据节点先收到hit回调，那么就应该用<code>forEachBasedTrie()</code>
     * 如果不需要每个T对应的字符串Key是什么，那么参数aNeedKey应该传递false以便更快地得到结果。
     * 如果hit的时候返回false那么遍历将终止。同一个父亲节点的儿子节点之间肯定按照字典序先后hit。
     *
     * @see forEachBasedTrie
     */
    public void forEachFast(boolean aNeedKey, Hit<T> aHit) {
        boolean whetherContinueHit = true;
        StringBuilder keyBuffer = aNeedKey ? new StringBuilder() : null;
        for (int i = 0, isize = this.mDatArray.length; whetherContinueHit && i < isize; ++i) {
            whetherContinueHit = this.traversalNextNode( i, keyBuffer, aNeedKey, aHit );
        }
    }

    /**
     * 按照Trie树父子关系bfs遍历，也就是父亲数据节点先收到hit回调，而且同一个父亲节点的儿子节点之间肯定按照字典序先后hit。
     *
     * @see forEachFast
     */
    public void forEachBasedTrie(boolean aNeedKey, Hit<T> aHit) {
        boolean whetherContinueHit = true;
        StringBuilder keyBuffer = aNeedKey ? new StringBuilder() : null;
        DoubleArrayTrieNode<T> [] datArray = this.mDatArray;
        int datArrayLength = this.mDatArray.length;
        //扫描一遍就建立trie结构，很快
        @SuppressWarnings("unchecked")
        LinkedList<Integer> [] trie = new LinkedList [ datArrayLength ];
        for (int i = 1; i < datArrayLength; ++i) {//从1开始，0是虚根
            DoubleArrayTrieNode<T> datNode = datArray[ i ];
            if (datNode != null) {
                //其实就是根据mCheck找到父亲节点建立父子关系
                int parentIndex = datNode.mCheck;
                LinkedList<Integer> childrenOfParentNode = trie[ parentIndex ];
                if (childrenOfParentNode == null) {
                    childrenOfParentNode = new LinkedList<Integer>();
                    trie[ parentIndex ] = childrenOfParentNode;
                }
                childrenOfParentNode.add( i );
            }
        }
        LinkedList<Integer> queue = new LinkedList<Integer>();
        queue.addLast( 0 );//从虚根开始进行深度优先遍历
        while (whetherContinueHit && !queue.isEmpty()) {
            Integer parentIndex = queue.removeFirst();
            if ((whetherContinueHit = this.traversalNextNode( parentIndex, keyBuffer, aNeedKey, aHit ))) {
                LinkedList<Integer> childrenIndex = trie[ parentIndex ];
                if (childrenIndex != null) {
                    for (Iterator<Integer> it = childrenIndex.iterator(); it.hasNext();) {
                        queue.addLast( it.next() );
                        it.remove();//fast GC it
                    }
                }
            }
        }
    }

    /**
     * 转换成急速多模式AhoCorasick调用方式
     */
    public DoubleArrayTrieAhoCorasick<T> asAhoCorasick() {
        DoubleArrayTrieAhoCorasick<T> ac = this.mAhoCorasick;
        if (ac == null) {
            ac = new DoubleArrayTrieAhoCorasick<T>( this );
            this.mAhoCorasick = ac;
        }
        return ac;
    }

    /**
     * 转成Map来用
     */
    public DoubleArrayTrieMap<T> asMap() {
        return new DoubleArrayTrieMap<T>( this );
    }

    //@ForDebugUse
    public void dump() {
        DoubleArrayTrieNode<T> [] datArray = this.mDatArray;
        for (int i = 0; i < datArray.length; i++) {
            DoubleArrayTrieNode<T> n = datArray[ i ];
            if (n != null) {
                char c = n.getChar( datArray, i );
                System.err.println( "i: " + i + " [" + n.mBase + ", " + n.mCheck + "]:" + c + ":" + n.mValue );
            }
        }
    }

    //@ForDebugUse
    public int getGapCount() {
        //看dat数据压缩情况：数中间空的个数
        return Arrays.stream( this.mDatArray ).mapToInt( (aNextNode) -> aNextNode == null ? 1 : 0 ).sum();
    }

    private boolean traversalNextNode(int aDatNodeIndex, StringBuilder aKeyBuffer, boolean aNeedKey, Hit<T> aHit) {
        int end = 0;
        DoubleArrayTrieNode<T> [] datArray = this.mDatArray;
        DoubleArrayTrieNode<T> aDatNode = datArray[ aDatNodeIndex ];
        if (aDatNode != null && aDatNode.mValue != null) {
            if (aNeedKey) {
                aKeyBuffer.setLength( 0 );
                //对数据节点回溯到根
                DoubleArrayTrieNode<T> ancestorNode = aDatNode;
                while (aDatNodeIndex != 0) {
                    aKeyBuffer.append( ancestorNode.getChar( datArray, aDatNodeIndex ) );
                    ancestorNode = datArray[ ancestorNode.mCheck ];
                }
                //反向一下，因为子孙节点对应的字符在前面，祖先节点对应的字符在后面
                aKeyBuffer.reverse();
                end = aKeyBuffer.length();
            }
            return aHit.hit( aKeyBuffer == null ? "" : aKeyBuffer, 0, end, aDatNode.mValue );
        }
        else {
            return true;
        }
    }

    static final class DoubleArrayTrieNode<T> {
        //虽然提供删除极为简单（只需要把节点的mCheck设置成无效-1),但没有必要,目的就是为了成为不变对象,以便可以任意的并发访问
        final int mBase;
        final int mCheck;
        final T mValue;

        DoubleArrayTrieNode(int aBase, int aCheck, T aValue) {
            this.mBase = aBase;
            this.mCheck = aCheck;
            this.mValue = aValue;
        }

        char getChar(DoubleArrayTrieNode<T> [] aDatArray, int aThisDatIndex) {
            //没有存储下标，也没有必要存储Unicode编码, 根据关系可简单的嘛
            //只是需要外部告诉本节点的 aThisDatIndex 才能计算出本节点的unicode编码值
            return ( char )(aThisDatIndex - aDatArray[ this.mCheck ].mBase);
        }
    }
}
