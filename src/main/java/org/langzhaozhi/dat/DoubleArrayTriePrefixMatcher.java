package org.langzhaozhi.dat;

import org.langzhaozhi.dat.DoubleArrayTrie.DoubleArrayTrieNode;

/**
 * <p>DAT的前缀结果匹配, 通过<code>DoubleArrayTrie.asPrefixMatcher()</code>来使用</p>
 * <p>不变对象，意味着一旦构造就不再改变，因此可以任意多线程并发访问。</p>
 * <p>概念化成两种类型的前缀匹配，调用者千万要注意区别：一种叫<前缀前匹配prefixBeforeMatch>
 * 另一种叫<前缀后匹配prefixAfterMatch>。为什么要严格化定义来区分这两种前缀匹配呢？原因是我们提出搜索匹配前缀结果的问题，
 * <b>到底是指所有结果的关键字串是输入字串的前缀呢，还是指输入字串是所有结果的关键字串的前缀?</b>
 * 这两句有点绕，举个例子可能就明白了，随带把概念以及概念的内涵外延进行清晰严格化的定义出来：</p>
 * <p>假设输入字串是"abcdefg"，而在DAT数据中只有 "ab","abcd","abcdef","abcdefg","abcdefgH","abcdefgHI","abcdefgHIJ"
 * 为关键字的数据的7个数据，那么<b>【所有结果的关键字串是输入字串的前缀】</b>就是前面4个数据 "ab","abcd","abcdef","abcdefg";
 * 而<b>【输入字串是所有结果的关键字串的前缀】</b>就是后面4个数据 "abcdefg","abcdefgH","abcdefgHI","abcdefgHIJ"。于是就有如下概念定义：<br/>
 * &#160;&#160;&#160;&#160;<b><前缀前匹配prefixBeforeMatch>：所有结果的关键字串是输入字串的前缀</b><br/>
 * &#160;&#160;&#160;&#160;<b><前缀后匹配prefixAfterMatch>：  输入字串是所有结果的关键字串的前缀</b><br/><br/>

 * 注意同输入字串相同的关键字数据 "abcdefg" 都在这两种结果当中，因为字串相同既符合<前缀前匹配prefixBeforeMatch>,也符合<前缀后匹配prefixAfterMatch>,
 * 当然，如果仅仅进行区分大小写的完全精确匹配，应该直接调用<code>DoubleArrayTrie::exactMatch</code>
 * 以得到更加极速的查询速度</p>
 * <p><前缀前匹配prefixBeforeMatch>和<前缀后匹配prefixAfterMatch>这两种类型的匹配，每种都分为字符大小写敏感(CaseSensitive)的匹配和非敏感(CaseInsensitive)的匹配，
 * 前者匹配速度快，用于全中文环境(中文无所谓大小写的嘛)或虽然英文但要求精确匹配的情况；后者匹配速度慢一点，一般是英文字母不区分大小写的情况。
 * 一般地，<b>建议构建数据尽可能精确化以便搜索匹配结果采用大小写敏感的方式来快速得到结果</b></p>
 * <p>由DAT的结构特征决定了DAT只适合前缀匹配，当然强行进行后缀搜索也是可以的，那就得忍受指数型的时间复杂度，这是根本行不通的。
 * 对偶方法提供了一种完美的解决思路，那什么是对偶呢？对偶表达的是两个对象之间的关系，此关系是对称的：A是B的对偶，那么B也是A的对偶，A进行连续两次对偶运算就是自身A。
 * 根据此定义来看，如果把DAT(这里称为正向DAT，即原始基准的DAT树)所有数据的关键字串进行前后倒置，重新形成一棵的DAT树(这里称为反向DAT)，那么这两棵DAT树的关系恰好就是对偶的，
 * 因此这个反向DAT就是正向DAT的对偶，称为对偶DAT。这样，<b>对对偶DAT树执行前缀匹配的时候本质上进行的是对正向DAT树执行后缀匹配，这恰好就是这里要要的结果!</b><br/>
 * 举例说明：假如正向DAT中只有 "ba","dcba","fedcba","gfedcba","Hgfedcba","IHgfedcba","JIHgfedcba"
 * 为关键字的数据的7个数据，现在输入字串是"gfedcba"，要求匹配出所有后缀结果。同前缀一样，这里同样要进行概念的明确化定义来明确到底什么是后缀匹配，
 * 也即同样有<后缀前匹配suffixBeforeMatch>和<后缀后匹配suffixAfterMatch>的这两种后缀匹配的对偶概念定义:<br/>
 * &#160;&#160;&#160;&#160;<b><后缀前匹配suffixBeforeMatch>：所有结果的关键字串是输入字串的后缀</b>本例中就是前面4个数据 "ba","dcba","fedcba","gfedcba"<br/>
 * &#160;&#160;&#160;&#160;<b><后缀后匹配suffixAfterMatch>：  输入字串是所有结果的关键字串的后缀</b>本例中就是后面4个数据 "gfedcba","Hgfedcba","IHgfedcba","JIHgfedcba"<br/><br/>
 *
 * 为了进行后缀匹配(继续本例)，例如<后缀前匹配suffixBeforeMatch>，按如下步骤：
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

    DoubleArrayTriePrefixMatcher(DoubleArrayTrie<T> aOwnerDat) {
        this.mOwnerDat = aOwnerDat;
    }

    /**
     * <p><b><前缀前匹配prefixBeforeMatch>：所有结果的关键字串是输入字串的前缀。</b>这是<b>大小写敏感</b>匹配。参见前面概念定义说明。</p>
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
        int keyCharLen = aInputText.length();
        if (keyCharLen == 0) {
            //空串对应虚根节点
            if (searchNode.mValue != null) {
                aHit.hit( aInputText, 0, 0, searchNode.mValue );
            }
        }
        else {
            for (int i = 0, datArrayLen = datArray.length; whetherContinueHit && i < keyCharLen; ++i) {
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
    }

    /**
     * <p><b><前缀前匹配prefixBeforeMatch>：所有结果的关键字串是输入字串的前缀。</b>这是<b>大小写非敏感</b>匹配,也即不区分大小写,速度稍慢。参见前面概念定义说明。</p>
     * <p>如果是对偶DAT，本方法本质上是<后缀前匹配suffixBeforeMatch>的大小写非敏感匹配实现,此时的输入字串aInputText也应该是正向DAT输入字串的对偶</p>
     * @param aInputText 输入字串
     * @param aHit 匹配后的回调
     */
    public void prefixBeforeMatchCaseInsensitive(CharSequence aInputText, Hit<T> aHit) {
        //todo...
    }

    /**
     * <p><b><前缀后匹配prefixAfterMatch>：输入字串是所有结果的关键字串的前缀。</b>这是<b>大小写敏感</b>匹配。参见前面概念定义说明。</p>
     * <p>如果是对偶DAT，本方法本质上是<后缀后匹配suffixAfterMatch>的大小写敏感匹配实现,此时的输入字串aInputText也应该是正向DAT输入字串的对偶</p>
     * @param aInputText 输入字串
     * @param aHit 匹配后的回调
     */
    public void prefixAfterMatchCaseSensitive(CharSequence aInputText, Hit<T> aHit) {
        //todo...
    }

    /**
     * <p><b><前缀后匹配prefixAfterMatch>：输入字串是所有结果的关键字串的前缀。</b>这是<b>大小写非敏感</b>匹配,也即不区分大小写,速度稍慢。参见前面概念定义说明。</p>
     * <p>如果是对偶DAT，本方法本质上是<后缀后匹配suffixAfterMatch>的大小写非敏感匹配实现,此时的输入字串aInputText也应该是正向DAT输入字串的对偶</p>
     * @param aInputText 输入字串
     * @param aHit 匹配后的回调
     */
    public void prefixAfterMatchCaseInsensitive(CharSequence aInputText, Hit<T> aHit) {
        //todo...
    }

    /**
     * 转换成DAT调用方式
     */
    public DoubleArrayTrie<T> asDoubleArrayTrie() {
        return this.mOwnerDat;
    }
}
