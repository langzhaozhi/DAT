package org.langzhaozhi.dat;

/**
 * 匹配到的通知回调，也就是匹配命中
 */
@FunctionalInterface
public interface Hit<T> {
    /**
     * 匹配命中词汇后的回调,通过返回true和false来决定是继续匹配(true)还是彻底中止匹配过程(false)
     *
     * @param aSearchText 匹配的模糊字符串
     * @param aStart 命中词汇在 aSearchText 中的起始包含位置，0表示从头开始
     * @param aEnd 命中词汇在 aSearchText 中的结束不包含位置，如果 aEnd == aSearchText.length() 就表示后缀
     * @param aValue 自动机中同命中词汇绑定在一起的数据对象
     *
     * @return 返回true表示继续遍历并可能继续收到通知,返回false表示停止整个匹配过程
     */
    public boolean hit(CharSequence aSearchText, int aStart, int aEnd, T aValue);

    /**
     * 过滤不是<完整词>的通知，这里的<完整词>的意思是以模式串aSearchText
     * 对于[aStart, aEnd)前后Character.isAlphabetic为判断基准
     */
    default Hit<T> asOnlyWholeWords() {
        return (aSearchText, aStart, aEnd, aValue) -> {
            if ((aStart == 0 || !Character.isAlphabetic( aSearchText.charAt( aStart - 1 ) )) && (aEnd == aSearchText.length() || !Character.isAlphabetic( aSearchText.charAt( aEnd ) ))) {
                return this.hit( aSearchText, aStart, aEnd, aValue );
            }
            else {
                return true;
            }
        };
    }
}
