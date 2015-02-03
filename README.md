# DAT
@Keywords(Java,Algorithm,PatternMatch,DoubleArrayTrie)

@Author(明月朗照之)

@CreatedDay(2015-01-19)

DoubleArrayTrie、DoubleArrayTriePrefixMatcher、DoubleArrayTrieAhoCorasick 这三者构成完整的DAT功能体系，
分别提供精确的完全匹配、前缀匹配、多模式串匹配。这里只提供前缀匹配，有关后缀匹配的问题可采用<b>对偶方式</b>
转换成前缀匹配方式完美解决。

DAT的构造过程比较慢，原因是为了尽可能压缩DAT数组长度令数据饱满，以后考虑研究一种既快速构造又大压缩的方法，
因此构造DAT最好能离线进行，构造好后进行数据持久化保存。之后在生产环境直接快速加载此持久化数据。


所有编辑文件都是UTF-8编码格式。基于JDK8，因为用lambda表达式编写代码很爽。
