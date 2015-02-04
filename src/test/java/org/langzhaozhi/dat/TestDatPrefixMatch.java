package org.langzhaozhi.dat;

import org.langzhaozhi.util.PairString;

/**
 * 测试前缀匹配功能功能，
 */
public class TestDatPrefixMatch {
    public static void main(String [] args) {
        String [] keys = {
            "ab", "abcd", "abcdef", "abcdefg", "abcdefgH", "abcdefgHI", "abcdefgHIJ"
        };
        @SuppressWarnings("unchecked")
        PairString<Integer> [] pairs = new PairString [ keys.length ];
        for (int i = 0; i < keys.length; ++i) {
            pairs[ i ] = new PairString<Integer>( keys[ i ], i );
        }
        DoubleArrayTrie<Integer> dat = DoubleArrayTrieMaker.makeDoubleArrayTrie( pairs );
        for (int i = 0; i < keys.length; ++i) {
            //先看DAT精确匹配情况
            if (dat.exactMatch( keys[ i ] ) != i) {
                throw new Error( "dat error:" + keys[ i ] );
            }
        }
        System.out.println( "=======输入串\"abcdefg\"：看下<前缀前匹配：大小写敏感>结果(结果关键字为输入串的前缀子串)，是否就是前面四个=====" );
        dat.asPrefixMatcher().prefixBeforeMatchCaseSensitive( "abcdefg", (aHitText, aStart, aEnd, aValue) -> {
            String resultKey = aHitText.subSequence( aStart, aEnd ).toString();
            System.out.println( "    匹配到关键字为[" + resultKey + "]的数据,对应的Attachment整数值为[" + aValue + "]" );
            return true;
        } );
        System.out.println( "\n=======输入串\"abcdefg\"：看下<前缀前匹配：大小写非敏感>结果(结果关键字为输入串的前缀子串)，是否也是前面四个=====" );
        dat.asPrefixMatcher().prefixBeforeMatchCaseInsensitive( "abcdefg", (aHitText, aStart, aEnd, aValue) -> {
            String resultKey = aHitText.subSequence( aStart, aEnd ).toString();
            System.out.println( "    匹配到关键字为[" + resultKey + "]的数据,对应的Attachment整数值为[" + aValue + "]" );
            return true;
        } );
        System.out.println( "\n=======输入串\"ABCDEFG\"：看下<前缀前匹配：大小写敏感>结果(结果关键字为输入串的前缀子串)，找不到结果才正确哦=====" );
        dat.asPrefixMatcher().prefixBeforeMatchCaseSensitive( "ABCDEFG", (aHitText, aStart, aEnd, aValue) -> {
            String resultKey = aHitText.subSequence( aStart, aEnd ).toString();
            throw new Error( "    居然匹配到关键字为[" + resultKey + "]的数据,对应的Attachment整数值为[" + aValue + "],说明程序有问题哦" );
        } );
        System.out.println( "===========================================================" );
        System.out.println( "\n=======输入串\"ABCDEFG\"：看下<前缀前匹配：大小写非敏感>结果(结果关键字为输入串的前缀子串)，是否就是前面四个=====" );
        dat.asPrefixMatcher().prefixBeforeMatchCaseInsensitive( "ABCDEFG", (aHitText, aStart, aEnd, aValue) -> {
            String resultKey = aHitText.subSequence( aStart, aEnd ).toString();
            System.out.println( "    匹配到关键字为[" + resultKey + "]的数据,对应的Attachment整数值为[" + aValue + "]" );
            return true;
        } );
        System.out.println();
        System.out.println();
        System.out.println( "=======输入串\"abcdefg\"：看下<前缀后匹配：大小写敏感>结果(结果关键字为输入串的前缀子串)，是否就是后面四个=====" );
        dat.asPrefixMatcher().prefixAfterMatchCaseSensitive( "abcdefg", (aHitText, aStart, aEnd, aValue) -> {
            String resultKey = aHitText.subSequence( aStart, aEnd ).toString();
            System.out.println( "    匹配到关键字为[" + resultKey + "]的数据,对应的Attachment整数值为[" + aValue + "]" );
            return true;
        } );
        System.out.println( "\n=======输入串\"abcdefg\"：看下<前缀后匹配：大小写非敏感>结果(结果关键字为输入串的前缀子串)，是否也是后面四个=====" );
        dat.asPrefixMatcher().prefixAfterMatchCaseInsensitive( "abcdefg", (aHitText, aStart, aEnd, aValue) -> {
            String resultKey = aHitText.subSequence( aStart, aEnd ).toString();
            System.out.println( "    匹配到关键字为[" + resultKey + "]的数据,对应的Attachment整数值为[" + aValue + "]" );
            return true;
        } );
        System.out.println( "\n=======输入串\"ABCDEFG\"：看下<前缀后匹配：大小写敏感>结果(结果关键字为输入串的前缀子串)，找不到结果才正确哦=====" );
        dat.asPrefixMatcher().prefixAfterMatchCaseSensitive( "ABCDEFG", (aHitText, aStart, aEnd, aValue) -> {
            String resultKey = aHitText.subSequence( aStart, aEnd ).toString();
            throw new Error( "    居然匹配到关键字为[" + resultKey + "]的数据,对应的Attachment整数值为[" + aValue + "],说明程序有问题哦" );
        } );
        System.out.println( "===========================================================" );
        System.out.println( "\n=======输入串\"ABCDEFG\"：看下<前缀后匹配：大小写非敏感>结果(结果关键字为输入串的前缀子串)，是否就是后面四个=====" );
        dat.asPrefixMatcher().prefixAfterMatchCaseInsensitive( "ABCDEFG", (aHitText, aStart, aEnd, aValue) -> {
            String resultKey = aHitText.subSequence( aStart, aEnd ).toString();
            System.out.println( "    匹配到关键字为[" + resultKey + "]的数据,对应的Attachment整数值为[" + aValue + "]" );
            return true;
        } );
    }
}
