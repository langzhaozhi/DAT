package org.langzhaozhi.dat;

import org.langzhaozhi.util.PairString;

/**
 * 测试DAT后缀匹配，采用对偶方法创建一个对偶的DAT，见 DoubleArrayTriePrefixMatcher 有关概念的详细介绍。
 * DAT的结构特征决定了只能进行前缀匹配，采用对偶方法完美地把后缀匹配变换成前缀匹配来实现，本例演示之。
 */
public class TestDatSuffixMatch {
    public static void main(String [] args) {
        String [] keys = {
            //后缀匹配的测试数据
            "ba", "dcba", "fedcba", "gfedcba", "Hgfedcba", "IHgfedcba", "JIHgfedcba"
        };
        @SuppressWarnings("unchecked")
        PairString<Integer> [] pairs = new PairString [ keys.length ];
        for (int i = 0; i < keys.length; ++i) {
            pairs[ i ] = new PairString<Integer>( keys[ i ], i );
        }

        //创建一个正向DAT，就是原始的基准DAT，目的就是要对它进行后缀匹配：实际是通过在对偶DAT上的前缀匹配来实现此功能的。
        DoubleArrayTrie<Integer> originDat = DoubleArrayTrieMaker.makeDoubleArrayTrie( pairs );
        //对正向DAT进行一次对偶运算：生成一个对偶DAT
        DoubleArrayTrie<Integer> dualDat = DoubleArrayTrieMaker.makeDoubleArrayTrieDual( pairs );

        /****注意，下面每一个后缀匹配测试都是都对输入字符串进行一次对偶运算即进行倒置反转后再作用于对偶DAT上,得到的结果又再对偶回来后，在正向DAT上查询真正的结果，就得到后缀匹配结果了********/

        System.out.println( "=======输入串\"gfedcba\"：看下<后缀前匹配：大小写敏感>结果(结果关键字为输入串的后缀子串)，是否就是前面四个=====" );
        TestDatSuffixMatch.suffixBeforeMatchCaseSensitive( originDat, dualDat, "gfedcba" );
        System.out.println( "\n=======输入串\"gfedcba\"：看下<后缀前匹配：大小写非敏感>结果(结果关键字为输入串的后缀子串)，是否也是前面四个=====" );
        TestDatSuffixMatch.suffixBeforeMatchCaseInsensitive( originDat, dualDat, "gfedcba" );
        System.out.println( "\n=======输入串\"GFEDCBA\"：看下<后缀前匹配：大小写敏感>结果(结果关键字为输入串的后缀子串)，找不到结果才正确哦=====" );
        TestDatSuffixMatch.suffixBeforeMatchCaseSensitive( originDat, dualDat, "GFEDCBA" );
        System.out.println( "===========================================================" );
        System.out.println( "\n=======输入串\"GFEDCBA\"：看下<后缀前匹配：大小写非敏感>结果(结果关键字为输入串的后缀子串)，是否就是前面四个=====" );
        TestDatSuffixMatch.suffixBeforeMatchCaseInsensitive( originDat, dualDat, "gfedcba" );
        System.out.println( "\n\n=======输入串\"gfedcba\"：看下<后缀后匹配：大小写敏感>结果(结果关键字为输入串的后缀子串)，是否就是后面四个=====" );
        TestDatSuffixMatch.suffixAfterMatchCaseSensitive( originDat, dualDat, "gfedcba" );
        System.out.println( "\n=======输入串\"gfedcba\"：看下<后缀后匹配：大小写非敏感>结果(结果关键字为输入串的后缀子串)，是否也是后面四个=====" );
        TestDatSuffixMatch.suffixAfterMatchCaseInsensitive( originDat, dualDat, "gfedcba" );
        System.out.println( "\n=======输入串\"GFEDCBA\"：看下<后缀后匹配：大小写敏感>结果(结果关键字为输入串的后缀子串)，找不到结果才正确哦=====" );
        TestDatSuffixMatch.suffixAfterMatchCaseSensitive( originDat, dualDat, "GFEDCBA" );
        System.out.println( "===========================================================" );
        System.out.println( "\n=======输入串\"GFEDCBA\"：看下<后缀后匹配：大小写非敏感>结果(结果关键字为输入串的后缀子串)，是否就是后面四个=====" );
        TestDatSuffixMatch.suffixAfterMatchCaseInsensitive( originDat, dualDat, "GFEDCBA" );
    }

    /**
     * <后缀前匹配：大小写敏感>
     * @param aOriginDat 正向DAT
     * @param aDualDat 对偶DAT
     */
    private static void suffixBeforeMatchCaseSensitive(DoubleArrayTrie<Integer> aOriginDat, DoubleArrayTrie<Integer> aDualDat, String aInputText) {
        StringBuilder dualInputText = new StringBuilder( aInputText ).reverse();//输入字串对偶化
        aDualDat.asPrefixMatcher().prefixBeforeMatchCaseSensitive( dualInputText, (aHitText, aStart, aEnd, aValue) -> {
            CharSequence dualResultKey = aHitText.subSequence( aStart, aEnd );
            Integer dualResult = aValue;
            String originResultKey = new StringBuilder( dualResultKey ).reverse().toString();//再次对偶化为正向DAT的结果字串
            Integer originAttachment = aOriginDat.exactMatch( originResultKey );
            System.out.println( "    匹配到关键字为[" + originResultKey + "]的数据,对应的Attachment整数值为[" + originAttachment + "],对偶运算是否正确：" + (originAttachment != null && (originAttachment == dualResult) ? "OK" : "Error") );
            return true;
        } );
    }

    /**
     * <后缀前匹配：大小写非敏感>  不区分大小写的情况
     * @param aOriginDat 正向DAT
     * @param aDualDat 对偶DAT
     */
    private static void suffixBeforeMatchCaseInsensitive(DoubleArrayTrie<Integer> aOriginDat, DoubleArrayTrie<Integer> aDualDat, String aInputText) {
        StringBuilder dualInputText = new StringBuilder( aInputText ).reverse();//输入字串对偶化
        aDualDat.asPrefixMatcher().prefixBeforeMatchCaseSensitive( dualInputText, (aHitText, aStart, aEnd, aValue) -> {
            CharSequence dualResultKey = aHitText.subSequence( aStart, aEnd );
            Integer dualResult = aValue;
            String originResultKey = new StringBuilder( dualResultKey ).reverse().toString();//再次对偶化为正向DAT的结果字串
            Integer originAttachment = aOriginDat.exactMatch( originResultKey );
            System.out.println( "    匹配到关键字为[" + originResultKey + "]的数据,对应的Attachment整数值为[" + originAttachment + "],对偶运算是否正确：" + (originAttachment != null && (originAttachment == dualResult) ? "OK" : "Error") );
            return true;
        } );
    }

    /**
     * <后缀后匹配：大小写敏感>
     * @param aOriginDat 正向DAT
     * @param aDualDat 对偶DAT
     */
    private static void suffixAfterMatchCaseSensitive(DoubleArrayTrie<Integer> aOriginDat, DoubleArrayTrie<Integer> aDualDat, String aInputText) {
        StringBuilder dualInputText = new StringBuilder( aInputText ).reverse();//输入字串对偶化
        aDualDat.asPrefixMatcher().prefixAfterMatchCaseSensitive( dualInputText, (aHitText, aStart, aEnd, aValue) -> {
            CharSequence dualResultKey = aHitText.subSequence( aStart, aEnd );
            Integer dualResult = aValue;
            String originResultKey = new StringBuilder( dualResultKey ).reverse().toString();//再次对偶化为正向DAT的结果字串
            Integer originAttachment = aOriginDat.exactMatch( originResultKey );
            System.out.println( "    匹配到关键字为[" + originResultKey + "]的数据,对应的Attachment整数值为[" + originAttachment + "],对偶运算是否正确：" + (originAttachment != null && (originAttachment == dualResult) ? "OK" : "Error") );
            return true;
        } );
    }

    /**
     * <后缀后匹配：大小写非敏感> 不区分大小写
     * @param aOriginDat 正向DAT
     * @param aDualDat 对偶DAT
     */
    private static void suffixAfterMatchCaseInsensitive(DoubleArrayTrie<Integer> aOriginDat, DoubleArrayTrie<Integer> aDualDat, String aInputText) {
        StringBuilder dualInputText = new StringBuilder( aInputText ).reverse();//输入字串对偶化
        aDualDat.asPrefixMatcher().prefixAfterMatchCaseInsensitive( dualInputText, (aHitText, aStart, aEnd, aValue) -> {
            CharSequence dualResultKey = aHitText.subSequence( aStart, aEnd );
            Integer dualResult = aValue;
            String originResultKey = new StringBuilder( dualResultKey ).reverse().toString();//再次对偶化为正向DAT的结果字串
            Integer originAttachment = aOriginDat.exactMatch( originResultKey );
            System.out.println( "    匹配到关键字为[" + originResultKey + "]的数据,对应的Attachment整数值为[" + originAttachment + "],对偶运算是否正确：" + (originAttachment != null && (originAttachment == dualResult) ? "OK" : "Error") );
            return true;
        } );
    }
}
