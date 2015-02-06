package org.langzhaozhi.dat;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;

import org.langzhaozhi.dat.DoubleArrayTrie.DoubleArrayTrieNode;
import org.langzhaozhi.util.IntHash;

/**
 * <p>DAT的前缀结果匹配, 通过<code>DoubleArrayTrie.asPrefixMatcher()</code>来使用</p>
 * <p>不变对象，意味着一旦构造就不再改变，因此可以任意多线程并发访问。</p>
 * <p>概念化成两种类型的前缀匹配，调用者千万要注意区别：一种叫<前缀前匹配prefixBeforeMatch>
 * 另一种叫<前缀后匹配prefixAfterMatch>。为什么要严格化定义来区分这两种前缀匹配呢？原因是我们提出搜索匹配前缀结果的问题，
 * <b>到底是指所有结果的关键字串是输入字串的前缀呢，还是指输入字串是所有结果的关键字串的前缀?</b>
 * 核心就到到底谁是谁的前缀?再举个例子可能就明白了，随带把概念以及概念的内涵外延进行清晰严格化的定义出来：</p>
 * <p>假设输入字串是"abcdefg"，而在DAT数据中只有 "ab","abcd","abcdef","abcdefg","abcdefgH","abcdefgHI","abcdefgHIJ"
 * 为关键字的数据的7个数据，那么<b>【所有结果的关键字串是输入字串的前缀】</b>就是前面4个数据 "ab","abcd","abcdef","abcdefg";
 * 而<b>【输入字串是所有结果的关键字串的前缀】</b>就是后面4个数据 "abcdefg","abcdefgH","abcdefgHI","abcdefgHIJ"。
 * 于是就有如下概念定义：<br/>
 * &#160;&#160;&#160;&#160;<b><前缀前匹配prefixBeforeMatch>：匹配出DAT数据的一个最大子集，使得此子集的每个数据的关键字串是输入字串的前缀</b><br/>
 * &#160;&#160;&#160;&#160;<b><前缀后匹配prefixAfterMatch>：  匹配出DAT数据的一个最大子集，使得输入字串是此子集的每个数据的关键字串的前缀</b><br/><br/>
 *
 * 这两句数学严格化定义的文字描述有点绕，其实用数学符号表示就简单多了，没办法，人类自然语言无论中文还是英文还是其它什么文都太落后了。
 * 简单说就是看到底谁是谁的前缀，是匹配结果的关键字串是输入字串的前缀，还是输入字串是匹配结果的关键字串的前缀!
 * 注意本例中同输入字串相同的关键字数据 "abcdefg" 都在这两种结果当中，因为字串相同既符合<前缀前匹配prefixBeforeMatch>,也符合<前缀后匹配prefixAfterMatch>,
 * 当然，如果仅仅进行区分大小写的完全精确匹配，应该直接调用<code>DoubleArrayTrie::exactMatch</code>
 * 以得到更加极速的查询速度</p>
 * <p><前缀前匹配prefixBeforeMatch>和<前缀后匹配prefixAfterMatch>这两种类型的匹配，每种都分为字符大小写敏感(CaseSensitive)的匹配和非敏感(CaseInsensitive)的匹配，
 * 前者匹配速度快，用于全中文环境(中文无所谓大小写的嘛)或虽然英文但要求精确匹配的情况；后者匹配速度慢一点，一般是英文字母不区分大小写的情况。
 * 一般地，<b>建议构建数据尽可能精确化以便搜索匹配结果采用大小写敏感的方式来快速得到结果</b></p>
 * <p>由DAT的结构特征决定了DAT只适合前缀匹配，当然强行进行后缀搜索也是可以的，那就得忍受指数型的时间复杂度，这是根本行不通的。
 * 对偶方法提供了一种完美的解决思路，那什么是对偶呢？目前尚没有见到这个概念的严格定义，包括维基上的堆砌文字也没有说清楚，本人给出如下严格定义：<br/>
 * <p>
 *      <b>对任意对象A和B，若存在一个函数f, 使得 f(A) = B 并且 f(B) = A，那么就称 A 为 f 下 B 的对偶，B 为 f 下 A 的对偶, 并称f为A和B的对偶函数或对偶运算</b>
 * </p>
 * <p>
 * 这里的函数f就是一种运算，本定义明确了对偶概念中运算f的核心地位，也就是对偶到底是相对于什么运算f而言的，而我们普通自然语言中经常说的对偶关系在这点上是含混不清的。
 * 在此定义下，自然地蕴涵如下意义：对偶表达的是两个对象之间的由运算f决定的关系，此关系是对称的：A是B在运算f下的对偶，那么B也是A在此运算f下的对偶，对A施行连续两次对偶运算f就是自身A。
 * 若A=B, 那运算f 本质上就是A 的不动点函数：f(A)=A
 * </p>
 * （本人确信上述定义是最严格精确的概念定义了，已经把上述定义提交到维基上【http://zh.wikipedia.org/wiki/对偶_(数学)】不知道是否会被那帮文字堆砌者采纳，无所谓了）
 * 根据此定义，先看字符串A上的一个运算f：前后倒置运算。此时 B=f(A) 就是字符串A 的倒置字符串，f对B施行运算即有 A = f(B)。完全符合上述对偶的定义：A和B就是倒置运算f下的彼此对偶。
 * 如果把DAT(这里称为正向DAT，即原始基准的DAT树)所有数据的关键字串进行前后倒置，重新形成一棵的DAT树(这里称为反向DAT)，那么这两棵DAT树的关系恰好也是在f下是彼此对偶的。
 * 因此这个反向DAT就是正向DAT的对偶，称为对偶DAT。这样，<b>对对偶DAT树执行前缀匹配的时候本质上进行的是对正向DAT树执行后缀匹配，这恰好就是这里要要的结果!</b><br/>
 * 举例说明：假如正向DAT中只有 "ba","dcba","fedcba","gfedcba","Hgfedcba","IHgfedcba","JIHgfedcba"
 * 为关键字的数据的7个数据，现在输入字串是"gfedcba"，要求匹配出所有后缀结果。同前缀一样，这里同样要进行概念的明确化定义来明确到底什么是后缀匹配，
 * 也即同样有<后缀前匹配suffixBeforeMatch>和<后缀后匹配suffixAfterMatch>的这两种后缀匹配的对偶概念定义:<br/>
 * &#160;&#160;&#160;&#160;<b><后缀前匹配suffixBeforeMatch>：匹配出DAT数据的一个最大子集，使得此子集的每个数据的关键字串是输入字串的后缀</b>本例中就是前面4个数据 "ba","dcba","fedcba","gfedcba"<br/>
 * &#160;&#160;&#160;&#160;<b><后缀后匹配suffixAfterMatch>：  匹配出DAT数据的一个最大子集，使得输入字串是此子集的每个数据的关键字串的后缀</b>本例中就是后面4个数据 "gfedcba","Hgfedcba","IHgfedcba","JIHgfedcba"<br/><br/>
 *
 * 核心理解也是看到底谁是谁的后缀。为了进行后缀匹配(继续本例)，例如<后缀前匹配suffixBeforeMatch>，按如下步骤：
 * <ol>
 * <li>对此正向DAT树生成一个对偶DAT树，此对偶DAT树包含如下7个数据："ab","abcd","abcdef","abcdefg","abcdefgH","abcdefgHI","abcdefgHIJ"</li>
 * <li>现在对输入字串"gfedcba"也进行对偶化，变换成对偶输入字串 "abcdefg"</li>
 * <li>在对偶DAT树上用对偶输入字串 "abcdefg"执行<前缀前匹配prefixBeforeMatch>,得到 "ab","abcd","abcdef","abcdefg" 的对应结果</li>
 * <li>对这些结果的关键字串再进行对偶化便得到在正向DAT中 "ba","dcba","fedcba","gfedcba" 对应的4个匹配结果</li>
 * </ol>
 * 执行<后缀后匹配suffixAfterMatch>也是相同的步骤只是第3步采用<前缀后匹配prefixAfterMatch>
 * 得到结果 "abcdefg","abcdefgH","abcdefgHI","abcdefgHIJ" 的对应结果，以及第4步对这些结果的关键字串再进行对偶化便得到在正向DAT中
 * "gfedcba","Hgfedcba","IHgfedcba","JIHgfedcba" 对应的4个匹配结果。
 * </p>
 * <p>注意AhoCorasick多模式串匹配也能匹配出所有<前缀前匹配prefixBeforeMatch>和<后缀前匹配suffixBeforeMatch>的结果，只是如果要专门进行前后缀匹配的话这里提供的方式效率更高，
 * 而AhoCorasick多模式串匹配提供不了<前缀后匹配prefixAfterMatch>和<后缀后匹配suffixAfterMatch>的结果，这点尤其要注意。</p>
 * <p>要生成对偶DAT，用<code>DoubleArrayTrieMaker::makeDoubleArrayTrieDual</code></p>
 */
public final class DoubleArrayTriePrefixMatcher<T> {
    private DoubleArrayTrie<T> mOwnerDat;

    //建立一个同DAT数组下标完全对应的Prefix的Trie树结构数组
    private PrefixTrieNode<T> [] mPrefixTrieArray;

    DoubleArrayTriePrefixMatcher(DoubleArrayTrie<T> aOwnerDat) {
        this.mOwnerDat = aOwnerDat;
    }

    /**
     * <p><b><前缀前匹配prefixBeforeMatch>：匹配结果的关键字串是输入字串的前缀。</b>这是<b>大小写敏感</b>匹配。参见前面概念定义说明。</p>
     * <p>如果是对偶DAT，本方法本质上是<后缀前匹配suffixBeforeMatch>的大小写敏感匹配实现,此时的输入字串aInputText也应该是正向DAT输入字串的对偶</p>
     * @param aInputText 输入字串
     * @param aHit 匹配后的回调
     */
    public void prefixBeforeMatchCaseSensitive(CharSequence aInputText, Hit<T> aHit) {
        boolean whetherContinueHit = true;
        DoubleArrayTrieNode<T> [] datArray = this.mOwnerDat.mDatArray;
        //总是从虚根开始
        DoubleArrayTrieNode<T> searchNode = datArray[ 0 ];
        int parentCheck = searchNode.mCheck;
        for (int i = 0, keyCharLen = aInputText.length(), datArrayLen = datArray.length; whetherContinueHit && i < keyCharLen; ++i) {
            char nextChar = aInputText.charAt( i );
            int index = searchNode.mBase + nextChar;
            if (index < 0 || index >= datArrayLen) {
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
                        whetherContinueHit = aHit.hit( aInputText, 0, i + 1, searchNode.mValue );
                    }
                    parentCheck = index;
                }
            }
        }
    }

    /**
     * <p><b><前缀前匹配prefixBeforeMatch>：匹配结果的关键字串是输入字串的前缀。</b>这是<b>大小写非敏感</b>匹配,也即不区分大小写,速度稍慢。参见前面概念定义说明。</p>
     * <p>如果是对偶DAT，本方法本质上是<后缀前匹配suffixBeforeMatch>的大小写非敏感匹配实现,此时的输入字串aInputText也应该是正向DAT输入字串的对偶</p>
     * @param aInputText 输入字串
     * @param aHit 匹配后的回调
     */
    public void prefixBeforeMatchCaseInsensitive(CharSequence aInputText, Hit<T> aHit) {
        DoubleArrayTrieNode<T> [] datArray = this.mOwnerDat.mDatArray;
        HashSet<Integer> parentCheckSet = new HashSet<Integer>();
        HashSet<Integer> thisCheckSet = new HashSet<Integer>();
        //记录真正匹配到的关键字串,以便正确 hit 回调的时候把DAT中对应的真正关键字进行通知
        IntHash<String> matchedKeys = new IntHash<String>( aInputText.length() );
        matchedKeys.put( 0, "" );//虚根对应空串
        parentCheckSet.add( datArray[ 0 ].mCheck );//always root as first parent
        for (int i = 0, keyCharLen = aInputText.length(), datArrayLen = datArray.length; i < keyCharLen; ++i) {
            char oneChar = aInputText.charAt( i );
            char twoChar = Character.isUpperCase( oneChar ) ? Character.toLowerCase( oneChar ) : (Character.isLowerCase( oneChar ) ? Character.toUpperCase( oneChar ) : oneChar);

            for (Iterator<Integer> parentIterator = parentCheckSet.iterator(); parentIterator.hasNext();) {
                int parentCheck = parentIterator.next();
                DoubleArrayTrieNode<T> parentDatNode = datArray[ parentCheck ];
                parentIterator.remove();//迭代清空
                String parentKey = matchedKeys.get( parentCheck );

                for (int j = 0, jsize = oneChar == twoChar ? 1 : 2; j < jsize; ++j) {
                    char nextChar = j == 0 ? oneChar : twoChar;
                    int index = parentDatNode.mBase + nextChar;
                    if (index < 0 || index >= datArrayLen) {
                        //nothing to do:由于mBase可能为负,因此这里计算出的index有可能在数组范围外
                    }
                    else {
                        DoubleArrayTrieNode<T> childDatNode = datArray[ index ];
                        if (childDatNode == null || childDatNode.mCheck != parentCheck) {
                            //nothing to do:check检查非常关键，如果check不相等，此 searchNode 肯定不是后继节点
                        }
                        else {
                            String childKey = parentKey + nextChar;
                            if (childDatNode.mValue != null) {
                                if (!aHit.hit( childKey, 0, i + 1, childDatNode.mValue )) {
                                    return;
                                }
                            }
                            thisCheckSet.add( index );//记录下一层的parentCheck
                            matchedKeys.put( index, childKey );
                        }
                    }
                }
            }
            if (thisCheckSet.isEmpty()) {
                //说明当前字符无论大小写都没有匹配到数据，后面的不用匹配了
                break;
            }
            else {
                HashSet<Integer> tmp = parentCheckSet;//已经被清空了的
                parentCheckSet = thisCheckSet;
                thisCheckSet = tmp;
            }
        }
    }

    /**
     * <p><b><前缀后匹配prefixAfterMatch>：输入字串是匹配结果的关键字串的前缀。</b>这是<b>大小写敏感</b>匹配。参见前面概念定义说明。</p>
     * <p>如果是对偶DAT，本方法本质上是<后缀后匹配suffixAfterMatch>的大小写敏感匹配实现,此时的输入字串aInputText也应该是正向DAT输入字串的对偶</p>
     * @param aInputText 输入字串
     * @param aHit 匹配后的回调
     */
    public void prefixAfterMatchCaseSensitive(CharSequence aInputText, Hit<T> aHit) {
        int keyCharLen = aInputText.length();
        if (keyCharLen == 0) {
            //不支持空串，因为全匹配实在是无意义的空耗
            return;
        }
        DoubleArrayTrieNode<T> [] datArray = this.mOwnerDat.mDatArray;
        //总是从虚根开始，先匹配出所有 aInputText 的前缀
        DoubleArrayTrieNode<T> searchNode = datArray[ 0 ];
        int parentCheck = searchNode.mCheck;
        for (int i = 0, datArrayLen = datArray.length; i < keyCharLen; ++i) {
            char nextChar = aInputText.charAt( i );
            int index = searchNode.mBase + nextChar;
            if (index < 0 || index >= datArrayLen) {
                //由于mBase可能为负,因此这里计算出的index有可能在数组范围外
                return;//直接结束，因为找不到任何数据使得输入字串是此数据的前缀
            }
            else {
                searchNode = datArray[ index ];
                if (searchNode == null || searchNode.mCheck != parentCheck) {
                    //check检查非常关键，如果check不相等，此 searchNode 肯定不是后继节点
                    return;//直接结束，因为找不到任何数据使得输入字串是此数据的前缀
                }
                else {
                    parentCheck = index;
                }
            }
        }
        //走到这里说明已经把输入字串aInputText每个字符都匹配到了,就从parentCheck位置的节点分支开始遍历所有子孙即可
        searchNode = datArray[ parentCheck ];
        if (searchNode.mValue != null) {
            //先通知自身相等串
            if (!aHit.hit( aInputText, 0, aInputText.length(), searchNode.mValue )) {
                return;
            }
        }
        PrefixTrieNode<T> [] prefixTrieArray = this.getPrefixTrieArray();
        PrefixTrieNode<T> branchRootPrefixNode = prefixTrieArray[ parentCheck ];
        StringBuilder keyCharBuffer = new StringBuilder( aInputText );
        if (branchRootPrefixNode.mChildrenIndexes != null) {
            branchRootPrefixNode.prefixAfterMatch( datArray, prefixTrieArray, keyCharBuffer, aHit );
        }
    }

    /**
     * <p><b><前缀后匹配prefixAfterMatch>：输入字串是匹配结果的关键字串的前缀。</b>这是<b>大小写非敏感</b>匹配,也即不区分大小写,速度稍慢。参见前面概念定义说明。</p>
     * <p>如果是对偶DAT，本方法本质上是<后缀后匹配suffixAfterMatch>的大小写非敏感匹配实现,此时的输入字串aInputText也应该是正向DAT输入字串的对偶</p>
     * @param aInputText 输入字串
     * @param aHit 匹配后的回调
     */
    public void prefixAfterMatchCaseInsensitive(CharSequence aInputText, Hit<T> aHit) {
        int keyCharLen = aInputText.length();
        if (keyCharLen == 0) {
            //不支持空串，因为全匹配实在是无意义的空耗
            return;
        }
        DoubleArrayTrieNode<T> [] datArray = this.mOwnerDat.mDatArray;
        HashSet<Integer> parentCheckSet = new HashSet<Integer>();
        HashSet<Integer> thisCheckSet = new HashSet<Integer>();
        IntHash<String> matchedKeys = new IntHash<String>( aInputText.length() );
        matchedKeys.put( 0, "" );//虚根对应空串
        parentCheckSet.add( datArray[ 0 ].mCheck );//always root as first parent
        for (int i = 0, datArrayLen = datArray.length; i < keyCharLen; ++i) {
            char oneChar = aInputText.charAt( i );
            char twoChar = Character.isUpperCase( oneChar ) ? Character.toLowerCase( oneChar ) : (Character.isLowerCase( oneChar ) ? Character.toUpperCase( oneChar ) : oneChar);

            for (Iterator<Integer> parentIterator = parentCheckSet.iterator(); parentIterator.hasNext();) {
                int parentCheck = parentIterator.next();
                DoubleArrayTrieNode<T> parentDatNode = datArray[ parentCheck ];
                parentIterator.remove();//迭代清空
                String parentKey = matchedKeys.get( parentCheck );

                for (int j = 0, jsize = oneChar == twoChar ? 1 : 2; j < jsize; ++j) {
                    char nextChar = j == 0 ? oneChar : twoChar;
                    int index = parentDatNode.mBase + nextChar;
                    if (index < 0 || index >= datArrayLen) {
                        //nothing to do:由于mBase可能为负,因此这里计算出的index有可能在数组范围外
                    }
                    else {
                        DoubleArrayTrieNode<T> childDatNode = datArray[ index ];
                        if (childDatNode == null || childDatNode.mCheck != parentCheck) {
                            //nothing to do:check检查非常关键，如果check不相等，此 searchNode 肯定不是后继节点
                        }
                        else {
                            thisCheckSet.add( index );//记录下一层的parentCheck
                            String childKey = parentKey + nextChar;
                            matchedKeys.put( index, childKey );
                        }
                    }
                }
            }
            if (thisCheckSet.isEmpty()) {
                //直接结束，因为找不到任何数据使得输入字串是此数据的前缀,而且是不区分大小写也找不到
                return;
            }
            else {
                HashSet<Integer> tmp = parentCheckSet;//已经被清空了的
                parentCheckSet = thisCheckSet;
                thisCheckSet = tmp;
            }
        }
        //走到这里说明已经把输入字串aInputText每个字符都匹配到了,就从parentCheckSet中位置的节点分支开始遍历所有子孙即可
        for (Integer parentCheck : parentCheckSet) {
            DoubleArrayTrieNode<T> searchNode = datArray[ parentCheck ];
            String realKey = matchedKeys.get( parentCheck );
            if (searchNode.mValue != null) {
                //先通知自身相等串,这里的相等可能是大小写非敏感意义下的相等，如a相等成A
                if (!aHit.hit( realKey, 0, aInputText.length(), searchNode.mValue )) {
                    return;
                }
            }
            PrefixTrieNode<T> [] prefixTrieArray = this.getPrefixTrieArray();
            PrefixTrieNode<T> branchRootPrefixNode = prefixTrieArray[ parentCheck ];
            StringBuilder keyCharBuffer = new StringBuilder( realKey );
            if (branchRootPrefixNode.mChildrenIndexes != null) {
                if (!branchRootPrefixNode.prefixAfterMatch( datArray, prefixTrieArray, keyCharBuffer, aHit )) {
                    return;
                }
            }
        }
    }

    private PrefixTrieNode<T> [] getPrefixTrieArray() {
        //只有<前缀后匹配prefixAfterMatch>的时候采用用到，因此采用延迟初始化构造整棵Trie结构
        //不变对象指内部结构或状态的实质改变，例如增删DAT数据，而这里建立Trie结构和改变DAT数据无关，
        //本质上是类似缓存用途性质的，因此依然符合不变对象。
        if (this.mPrefixTrieArray == null) {
            //扫描一遍就建立trie结构，很快
            DoubleArrayTrieNode<T> [] datArray = this.mOwnerDat.mDatArray;
            int datArrayLength = datArray.length;
            @SuppressWarnings("unchecked")
            PrefixTrieNode<T> [] prefixTrieArray = new PrefixTrieNode [ datArrayLength ];
            for (int i = 0; i < datArrayLength; ++i) {
                if (datArray[ i ] != null) {
                    prefixTrieArray[ i ] = new PrefixTrieNode<T>();
                }
            }
            @SuppressWarnings("unchecked")
            LinkedList<Integer> [] trie = new LinkedList [ datArrayLength ];
            for (int i = 1; i < datArrayLength; ++i) {//从1开始，0是虚根
                DoubleArrayTrieNode<T> parentDatNode = datArray[ i ];
                if (parentDatNode != null) {
                    //其实就是根据mCheck找到父亲节点建立父子关系
                    int parentIndex = parentDatNode.mCheck;
                    LinkedList<Integer> childrenOfParentNode = trie[ parentIndex ];
                    if (childrenOfParentNode == null) {
                        childrenOfParentNode = new LinkedList<Integer>();
                        trie[ parentIndex ] = childrenOfParentNode;
                    }
                    childrenOfParentNode.add( i );
                }
            }
            LinkedList<Integer> indexQueue = new LinkedList<Integer>();
            indexQueue.addLast( 0 );
            while (!indexQueue.isEmpty()) {
                Integer parentIndex = indexQueue.removeFirst();
                LinkedList<Integer> childrenIndexesList = trie[ parentIndex ];
                int [] childrenIndexes = new int [ childrenIndexesList.size() ];
                int from = 0;
                for (Iterator<Integer> it = childrenIndexesList.iterator(); it.hasNext();) {
                    Integer nextChildIndex = it.next();
                    if (trie[ nextChildIndex ] != null) {
                        indexQueue.addLast( nextChildIndex );
                    }
                    childrenIndexes[ from++ ] = nextChildIndex;
                    it.remove();//fast GC it
                }
                //建立Trie的父子关系
                prefixTrieArray[ parentIndex ].mChildrenIndexes = childrenIndexes;
            }
            this.mPrefixTrieArray = prefixTrieArray;
        }
        return this.mPrefixTrieArray;
    }

    /**
     * 转换成DAT调用方式
     */
    public DoubleArrayTrie<T> asDoubleArrayTrie() {
        return this.mOwnerDat;
    }

    static final class PrefixTrieNode<T> {
        //各个儿子在DAT数组中的下标,形成一棵完整的Trie树结构,目的是进行快速的层层遍历
        int [] mChildrenIndexes;

        PrefixTrieNode() {
        }

        boolean prefixAfterMatch(DoubleArrayTrieNode<T> [] aDatArray, PrefixTrieNode<T> [] aPrefixArray, StringBuilder aKeyCharBuffer, Hit<T> aHit) {
            int [] childrenIndexes = this.mChildrenIndexes;
            int keyCharLength = aKeyCharBuffer.length();
            int childKeyCharLength = keyCharLength + 1;
            for (int i = 0, childCount = childrenIndexes.length; i < childCount; ++i) {
                int nextChildNodeIndex = childrenIndexes[ i ];
                DoubleArrayTrieNode<T> nextChildDatNode = aDatArray[ nextChildNodeIndex ];
                PrefixTrieNode<T> nextChildPrefixNode = aPrefixArray[ nextChildNodeIndex ];

                char cc = aDatArray[ nextChildNodeIndex ].getChar( aDatArray, nextChildNodeIndex );
                aKeyCharBuffer.append( cc );
                if (nextChildDatNode.mValue != null) {
                    if (!aHit.hit( aKeyCharBuffer, 0, childKeyCharLength, nextChildDatNode.mValue )) {
                        return false;
                    }
                }
                if (nextChildPrefixNode.mChildrenIndexes != null) {
                    if (!nextChildPrefixNode.prefixAfterMatch( aDatArray, aPrefixArray, aKeyCharBuffer, aHit )) {
                        return false;
                    }
                }
                aKeyCharBuffer.setLength( keyCharLength );
            }
            return true;
        }
    }
}
