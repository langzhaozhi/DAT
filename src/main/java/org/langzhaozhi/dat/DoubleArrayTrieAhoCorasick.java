package org.langzhaozhi.dat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import org.langzhaozhi.dat.DoubleArrayTrie.DoubleArrayTrieNode;

/**
 * <p>极速多模式串匹配,基于 Aho-Corasick</p>
 * <p>不变对象，意味着一旦构造就不再改变，因此可以任意多线程并发访问。</p>
 * <p>多模式串匹配的定义是：<b>给定一个输入字串（或称为模糊输入表达式），匹配出所有结果数据使得这些结果数据的关键字串都是此输入字串的子串。</b>
 * 根据此定义，多模式串匹配将匹配出所有<前缀前匹配prefixBefore>和所有的<后缀前匹配suffixBefore>(参见<code>DoubleArrayTriePrefixMatcher</code>中的概念定义)，
 * 还包括完整精确匹配的结果，以及其他所有关键字串是输入字串的子串的数据结果。之所以前缀后缀匹配的结果有限而非所有完整的前缀后缀结果，从定义上认清谁的子串就清楚了。
 * 如果想要<前缀后匹配prefixAfter>或<后缀后匹配suffixAfter>，那么应该使用<code>DoubleArrayTriePrefixMatcher</code>或其对偶。</p>
 * <p>提供了对输入字串（模糊输入表达式）进行大小写敏感的匹配方式还是大小写非敏感的匹配方式，一般地，建议构建数据尽量精确化使得匹配尽量采用速度更快的大小写敏感匹配方式，
 * 例如如果数据全部都是中文汉字（无所谓大小写），那么只采用大小写敏感匹配方式就够了，建议尽量这么做，只有数据中存在英文字母大小写混杂且搜索确实要求非敏感匹配时，
 * 才应该用非敏感匹配方式，建议尽量不要怎么做</p>
 */
public final class DoubleArrayTrieAhoCorasick<T> {
    private DoubleArrayTrie<T> mOwnerDat;
    private AhoCorasickStateNode [] mStateNodeArray;

    DoubleArrayTrieAhoCorasick(DoubleArrayTrie<T> aOwnerDat) {
        this.mOwnerDat = aOwnerDat;
        this.constructFailureStates();
    }

    /**
     * 转换成DAT调用方式
     */
    public DoubleArrayTrie<T> asDoubleArrayTrie() {
        return this.mOwnerDat;
    }

    /**
     * AC模式匹配：字符大小写敏感的匹配，例如abc和ABC是不同的
     */
    public void matchCaseSensitive(CharSequence aMatcherText, Hit<T> aHit) {
        //大小写敏感匹配,很简单，对每个字符对应于一个failure转移
        boolean whetherContinueHit = true;
        AhoCorasickStateNode [] stateNodeArray = DoubleArrayTrieAhoCorasick.this.mStateNodeArray;
        AhoCorasickStateNode rootStateNode = stateNodeArray[ 0 ];
        AhoCorasickStateNode currentStateNode = rootStateNode;//从虚根开始
        for (int i = 0, count = aMatcherText.length(); whetherContinueHit && i < count; ++i) {
            char nextChar = aMatcherText.charAt( i );
            currentStateNode = currentStateNode.nextTransitionState( nextChar );
            whetherContinueHit = currentStateNode.tryHitCaseSensitive( i, aMatcherText, aHit );
        }
    }

    /**
     * AC模式匹配：字符大小写非敏感的匹配,例如abc可以匹配到ABC,aBc等
     */
    public void matchCaseInsensitive(CharSequence aMatcherText, Hit<T> aHit) {
        //大小写不敏感匹配，非常复杂
        //第一步：先转换不同的大小写字符
        int matcherTextCharCount = aMatcherText.length();
        char [] twoChars = new char [ matcherTextCharCount ];
        boolean whetherSame = true;
        for (int i = 0; i < matcherTextCharCount; ++i) {
            char nextChar = aMatcherText.charAt( i );
            char otherCaseChar = Character.isUpperCase( nextChar ) ? Character.toLowerCase( nextChar ) : (Character.isLowerCase( nextChar ) ? Character.toUpperCase( nextChar ) : nextChar);
            twoChars[ i ] = otherCaseChar;
            whetherSame = otherCaseChar != nextChar ? false : whetherSame;
        }
        if (whetherSame) {
            //显然由于每个字符大小写都一样，因此就直接用最简单的敏感匹配
            this.matchCaseSensitive( aMatcherText, aHit );
            return;
        }
        HashSet<Integer> repeatStartPosSet = new HashSet<Integer>();
        HashSet<Integer> preLayerSet = new HashSet<Integer>();
        HashSet<Integer> thisLayerSet = new HashSet<Integer>();
        AhoCorasickStateNode [] stateNodeArray = this.mStateNodeArray;
        AhoCorasickStateNode rootStateNode = stateNodeArray[ 0 ];//从虚根开始
        preLayerSet.add( rootStateNode.mThisDatIndex );
        for (int i = 0; i < matcherTextCharCount; ++i) {
            char oneChar = aMatcherText.charAt( i );
            char twoChar = twoChars[ i ];

            for (Iterator<Integer> preIt = preLayerSet.iterator(); preIt.hasNext();) {
                AhoCorasickStateNode preStateNode = stateNodeArray[ preIt.next() ];
                //迭代后清空preLayerSet
                preIt.remove();

                thisLayerSet.add( preStateNode.nextTransitionState( oneChar ).mThisDatIndex );
                if (twoChar != oneChar) {
                    thisLayerSet.add( preStateNode.nextTransitionState( twoChar ).mThisDatIndex );
                }
            }
            repeatStartPosSet.clear();
            for (Integer nextLayer : thisLayerSet) {
                if (!stateNodeArray[ nextLayer ].tryHitCaseInsensitive( i, aMatcherText, repeatStartPosSet, aHit )) {
                    //结束匹配任务
                    return;
                }
            }
            //下面转到下一个字符,此时 preLayerSet 已经被清空了的
            HashSet<Integer> tmp = preLayerSet;
            preLayerSet = thisLayerSet;
            thisLayerSet = tmp;
        }
    }

    /**
     * 建立failure表
     */
    private void constructFailureStates() {
        DoubleArrayTrieNode<T> [] datArray = this.mOwnerDat.mDatArray;
        //建立一个同DAT数组下标完全对应的状态表,专门处理AC状态迁移
        AhoCorasickStateNode [] stateNodeArray = DoubleArrayTrieAhoCorasick.newArray( datArray.length );//居然不能new AhoCorasickStateNode[ datArray.length ];
        this.mStateNodeArray = stateNodeArray;
        //创建对应虚根，其dat数组下标和深度都是0
        AhoCorasickStateNode rootStateNode = new AhoCorasickStateNode( 0 );
        stateNodeArray[ 0 ] = rootStateNode;
        //虚根是第0层，即深度为0
        rootStateNode.mDepth = 0;
        //虚根的failure指向自己
        rootStateNode.mFailureDatIndex = 0;
        //第一步: 扫描一遍建立临时用的Trie树,用数组下标来建立对应父子关系比hash表快太多了
        @SuppressWarnings("unchecked")
        LinkedList<AhoCorasickStateNode> [] trie = new LinkedList [ datArray.length ];
        for (int i = 1, datArrayLength = datArray.length; i < datArrayLength; ++i) {//从1开始，0是虚根
            if (datArray[ i ] != null) {
                AhoCorasickStateNode childStateNode = new AhoCorasickStateNode( i );
                stateNodeArray[ i ] = childStateNode;
                //其实就是根据mCheck找到父亲节点建立父子关系
                int parentIndex = datArray[ i ].mCheck;
                LinkedList<AhoCorasickStateNode> childrenOfParentNode = trie[ parentIndex ];
                if (childrenOfParentNode == null) {
                    childrenOfParentNode = new LinkedList<AhoCorasickStateNode>();
                    trie[ parentIndex ] = childrenOfParentNode;
                }
                childrenOfParentNode.add( childStateNode );
            }
        }
        //第二步: 将深度为1的节点的failure设为虚根节点下标0, 同时把它们中的非叶子节点加入到bfs遍历队列
        LinkedList<AhoCorasickStateNode> queue = new LinkedList<AhoCorasickStateNode>();
        for (AhoCorasickStateNode firstDepthStateNode : trie[ 0 ]) {
            firstDepthStateNode.mDepth = 1;
            firstDepthStateNode.mFailureDatIndex = 0;
            if (trie[ firstDepthStateNode.mThisDatIndex ] != null) {
                //排除第一层中本身是叶子的节点
                queue.addLast( firstDepthStateNode );
            }
        }
        //第三步: 为除了虚根和第1层外的其他节点建立failure表，这是一个bfs遍历方式
        while (!queue.isEmpty()) {
            AhoCorasickStateNode parentStateNode = queue.removeFirst();
            int childrenDepth = parentStateNode.mDepth + 1;
            LinkedList<AhoCorasickStateNode> children = trie[ parentStateNode.mThisDatIndex ];
            //为其下一层每个Success节点建立failure表
            for (AhoCorasickStateNode nextChildStateNode : children) {
                //对应的状态转移码
                char transitionChar = nextChildStateNode.getChar();
                AhoCorasickStateNode traceFailureState = stateNodeArray[ parentStateNode.mFailureDatIndex ];
                AhoCorasickStateNode newFailureState = traceFailureState.nextTransitionState( transitionChar );
                nextChildStateNode.mDepth = childrenDepth;
                nextChildStateNode.mFailureDatIndex = newFailureState.mThisDatIndex;
                //add bfs遍历,排除本身是叶子的节点
                if (trie[ nextChildStateNode.mThisDatIndex ] != null) {
                    queue.addLast( nextChildStateNode );
                }
            }
        }
    }

    @SafeVarargs
    static <E> E [] newArray(int aNewSize, E... aArray) {
        //居然不能 new AhoCorasickStateNode[arrayLength] 数组，只能用这个怪异方法了
        return Arrays.copyOf( aArray, aNewSize );
    }

    final class AhoCorasickStateNode {
        //本节点对应于DAT数组中的下标
        final int mThisDatIndex;
        //本节点对应的树的深度，在这里的含义就是从虚根到本节点字符匹配到的字符串长度，
        //本身是冗余信息没有必要存在的，因为可通过上溯到虚根计算得到，但为了加快访问速度还是做缓存
        int mDepth;
        //failure状态对应于DAT数组的下标
        int mFailureDatIndex;

        AhoCorasickStateNode(int aDatIndex) {
            this.mThisDatIndex = aDatIndex;
        }

        /**
         * 获取本节点对应的unicode编码
         */
        char getChar() {
            DoubleArrayTrieNode<T> [] datArray = DoubleArrayTrieAhoCorasick.this.mOwnerDat.mDatArray;
            int thisDatIndex = this.mThisDatIndex;
            return datArray[ thisDatIndex ].getChar( datArray, thisDatIndex );
        }

        /**
         * 转移到下一个状态（基于success转移）
         * @param aTransitionChar 希望按此字符转移
         * @return 转移结果,返回为null说明 aTransitionChar 不是本节点的 success 状态码
         */
        AhoCorasickStateNode trySuccessState(char aTransitionChar) {
            DoubleArrayTrieNode<T> [] datArray = DoubleArrayTrieAhoCorasick.this.mOwnerDat.mDatArray;
            DoubleArrayTrieNode<T> thisDatNode = datArray[ this.mThisDatIndex ];
            int transitionIndex = thisDatNode.mBase + aTransitionChar;
            if (transitionIndex < 0 || transitionIndex >= datArray.length) {
                //由于mBase可能为负,因此这里计算出的index有可能在数组范围外
                return null;
            }
            DoubleArrayTrieNode<T> successDatNode = datArray[ transitionIndex ];
            if (successDatNode == null || successDatNode.mCheck != this.mThisDatIndex) {
                //不是真的success节点
                return null;
            }
            else {
                return DoubleArrayTrieAhoCorasick.this.mStateNodeArray[ transitionIndex ];
            }
        }

        AhoCorasickStateNode nextTransitionState(char aTransitionChar) {
            //总是先尝试用success表转移
            AhoCorasickStateNode parentStateNode = this;
            AhoCorasickStateNode transitionStateNode = this.trySuccessState( aTransitionChar );
            while (transitionStateNode == null) {
                //不能success才用failure表转移,直到上溯到虚根节点
                if (parentStateNode.mThisDatIndex == 0) {
                    transitionStateNode = parentStateNode;
                }
                else {
                    parentStateNode = DoubleArrayTrieAhoCorasick.this.mStateNodeArray[ parentStateNode.mFailureDatIndex ];
                    transitionStateNode = parentStateNode.nextTransitionState( aTransitionChar );
                }
            }
            return transitionStateNode;
        }

        boolean tryHitCaseSensitive(int aPosition, CharSequence aMatcherText, Hit<T> aHit) {
            DoubleArrayTrieNode<T> [] datArray = DoubleArrayTrieAhoCorasick.this.mOwnerDat.mDatArray;
            AhoCorasickStateNode [] stateNodeArray = DoubleArrayTrieAhoCorasick.this.mStateNodeArray;
            AhoCorasickStateNode currentStateNode = this;
            while (currentStateNode.mThisDatIndex != 0) {
                DoubleArrayTrieNode<T> currentDatNode = datArray[ currentStateNode.mThisDatIndex ];
                if (currentDatNode.mValue != null) {
                    int startTextIndex = aPosition - currentStateNode.mDepth + 1;
                    if (!aHit.hit( aMatcherText, startTextIndex, aPosition + 1, currentDatNode.mValue )) {
                        //停止hit通知，结束任务了
                        return false;
                    }
                }
                //failure继续上溯直到虚根
                currentStateNode = stateNodeArray[ currentStateNode.mFailureDatIndex ];
            }
            return true;
        }

        boolean tryHitCaseInsensitive(int aPosition, CharSequence aMatcherText, Set<Integer> aRepeatSet, Hit<T> aHit) {
            //大小写不敏感时很容易重复匹配到相同的 (start,end)对，因此使用 aRepeatSet 来剔除重复的
            DoubleArrayTrieNode<T> [] datArray = DoubleArrayTrieAhoCorasick.this.mOwnerDat.mDatArray;
            AhoCorasickStateNode [] stateNodeArray = DoubleArrayTrieAhoCorasick.this.mStateNodeArray;
            AhoCorasickStateNode currentStateNode = this;
            while (currentStateNode.mThisDatIndex != 0) {
                DoubleArrayTrieNode<T> currentDatNode = datArray[ currentStateNode.mThisDatIndex ];
                if (currentDatNode.mValue != null) {
                    Integer startTextIndex = aPosition - currentStateNode.mDepth + 1;
                    if (!aRepeatSet.contains( startTextIndex )) {
                        //剃掉相同(start,end)完全重复的
                        aRepeatSet.add( startTextIndex );
                        if (!aHit.hit( aMatcherText, startTextIndex, aPosition + 1, currentDatNode.mValue )) {
                            return false;
                        }
                    }
                }
                //failure继续上溯直到虚根
                currentStateNode = stateNodeArray[ currentStateNode.mFailureDatIndex ];
            }
            return true;
        }
    }
}
