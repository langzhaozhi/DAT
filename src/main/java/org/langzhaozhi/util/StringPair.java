package org.langzhaozhi.util;

/**
 * 提供一个以CharSequence为key 的Key-Value对，key 的类型为 CharSequence 除了支持最常用的String外，
 * 还尤其支持大共享数据区的java.nio.CharBuffer 的使用，使得需要临时创建超大量轻量级 java.nio.CharBuffer
 * 实例(共享数据区)而不用占用太多内存
 *
 * @param <T>
 */
public final class StringPair<T> implements Comparable<StringPair<T>> {
    public final CharSequence mKey;
    public final T mValue;

    public StringPair(CharSequence aString, T aValue) {
        this.mKey = aString;
        this.mValue = aValue;
    }

    @Override
    public int compareTo(StringPair<T> aOther) {
        CharSequence thisKey = this.mKey;
        CharSequence otherKey = aOther.mKey;
        int len1 = thisKey.length();
        int len2 = otherKey.length();
        int min = len1 <= len2 ? len1 : len2;//Math.min(len1, len2);

        for (int k = 0; k < min; ++k) {
            char c1 = thisKey.charAt( k );
            char c2 = otherKey.charAt( k );
            if (c1 != c2) {
                return c1 - c2;
            }
        }
        return len1 - len2;
    }

    @Override
    public String toString() {
        return this.mKey.toString();
    }
}
