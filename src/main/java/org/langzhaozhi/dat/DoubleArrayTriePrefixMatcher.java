package org.langzhaozhi.dat;

import org.langzhaozhi.dat.DoubleArrayTrie.DoubleArrayTrieNode;

/**
 * <p>DAT的前缀结果匹配</p>
 * <p>不变对象，意味着一旦构造就不再改变，因此可以任意多线程并发访问。</p>
 * <p>概念化成两种类型的前缀匹配，这是完全相反的两种匹配方式，调用者千万要注意区别：一种叫<前缀前匹配prefixBefore>
 * 另一种叫<前缀后匹配prefixAfter>。为什么要严格化定义来区分这两种前缀匹配呢？原因是我们提出搜索匹配前缀结果的问题，
 * <b>到底是指所有结果的关键字串是输入字串的前缀呢，还是指输入字串是所有结果的关键字串的前缀?</b>
 * 这两句有点绕，举个例子可能就明白了，随带把概念以及概念的内涵外延进行清晰严格化的定义出来：</p>
 * <p>假设输入字串是"abcdefg"，而在DAT数据中只有 "ab","abcd","abcdef","abcdefg","abcdefgH","abcdefgHI","abcdefgHIJ"
 * 为关键字的数据的7个数据，那么<b>【所有结果的关键字串是输入字串的前缀】</b>就是前面4个数据，这种匹配方式被定义为<前缀前匹配prefixBefore>，
 * 而<b>【输入字串是所有结果的关键字串的前缀】</b>就是后面4个数据，这种匹配方式被定义成<前缀后匹配prefixAfter>。
 * 注意同输入字串相同的关键字数据 "abcdefg" 都在这两种结果当中，因为字串相同既符合<前缀前匹配prefixBefore>,也符合<前缀后匹配prefixAfter>,
 * 当然，如果仅仅进行区分大小写的完全精确匹配，应该直接调用<code>DoubleArrayTrie::exactMatch</code>
 * 以得到更加极速的查询速度</p>
 * <p><前缀前匹配prefixBefore>和<前缀后匹配prefixAfter>这两种类型的匹配，每种都分为字符大小写敏感(CaseSensitive)的匹配和非敏感(CaseInsensitive)的匹配，
 * 前者匹配速度快，用于全中文环境(中文无所谓大小写的嘛)或虽然英文但要求精确匹配的情况；后者匹配速度慢一点，一般是英文字母不区分大小写的情况。
 * 一般地，<b>建议构建数据尽可能精确化以便搜索匹配结果采用大小写敏感的方式来快速得到结果</b></p>
 * <p>本类名称虽然是前缀匹配，但采用<b>对偶方式</b>本类可用于后缀匹配，也即同样可以有<后缀前匹配suffixBefore>和<后缀后匹配suffixAfter>的匹配结果，
 * 这。。。。。。</p>
 * <p>http://zh.wikipedia.org/对偶_(数学)</p>
 */
public final class DoubleArrayTriePrefixMatcher<T> {
    private DoubleArrayTrie<T> mOwnerDat;

    DoubleArrayTriePrefixMatcher(DoubleArrayTrie<T> aOwnerDat) {
        this.mOwnerDat = aOwnerDat;
    }

    /**
     * <前缀前匹配prefixBefore>，大小写敏感匹配，参见前面概念定义说明。如果是对偶DAT，本方法本质上是实现<后缀前匹配suffixBefore>
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

    public void prefixBeforeMatchCaseInsensitive(CharSequence aInputText, Hit<T> aHit) {
        //todo...
    }

    public void prefixAfterMatchCaseSensitive(CharSequence aInputText, Hit<T> aHit) {
        //todo...
    }

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
