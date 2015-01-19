package org.langzhaozhi.dat;

import java.util.Arrays;

/**
 * <p>动态可变的Trie树结构，用于边插入数据边查询数据是否已经插入，二分法查找插入速度，非线程安全的，应该确保构造过程是单线程中进行的</p>
 * <p>比普通的HashMap快一些，占用内存空间更少（原因是数据量不定的情况下HashMap会不停地重新构造Hash结构或频繁的hash函数运算，
 * 要不就只能初始预留空间特别大，使得尽可能使每个对象拥有不同的hash），Trie特别适合于边构造边检查的应用情况</p>
 * <p>构造完毕后，也就是数据不再变化时，可通过<code>toDoubleArrayTrie()</code>方法转换成双数组DAT树来提供更加极速的查询访问。注意，
 * <code>toDoubleArrayTrie()</code>调用后的DAT数据是调用时刻的一个镜像，也就是之后如果再对此Trie树做修改如增加数据等，
 * 则对已经生成的DAT没有任何影响，这就是为什么方法名叫<code>toDoubleArrayTrie()</code>而不叫"asDoubleArrayTrie()"的原因</p>
 * <p>如果仅仅是为了构造DAT来使用并且这些数据是预先就确定了的静态数据的话，那么采用<code>DoubleArrayTrieMaker.makeDoubleArrayTrie()</code>
 * 应该是更适合的方法，因为其整体构造速度更快。而Trie的构造过程则需要是对每一个节点数据都要二分查找到具体位置于哪里进行插入才合适，
 * 虽然二分查找时间复杂度已经极好了O(logN),不过无论如何也远远比不上双数组DAT那种直接对应数组下标来的快</p>
 * <p>一点小总结：如何衡量Hash的速度?</p>
 * <p>
 *     <ol>
 *         <li>第一类：恒等hash，简单地说就是一个对象自己是自己的恒等hash，没有比这更快的了,理解成直接指针和对象</li>
 *         <li>第二类：一一对应hash，就是一对象集的各元素与一个相同基数的序数集的各元素建立一一映射关系
 *             (有点绕口，基数、序数与个数这些概念的共性和差异要看康托尔理论)，我们编写程序就是把一类可列有限集同阿列夫0(就是自然数集嘛)
 *             的一个有限子集建立一一映射来用的：数组元素和数组下标的一一对应映射关系，速度当然也是O(1)，
 *             所以能用简单数组下标建立一一对应就不要错过。随便说一句：一一对应的概念是人类的第一个理性知识，是计数的基础。
 *         </li>
 *         <li>第三类：算法类hash,就是我们平常说的hash了，各种算法五花八门（二分查找、移位），归结一句话：都比不上第二类一一对应
 *             hash简单直接有效，但前提是能直接了当地定义出一一对应hash，有时往往缺乏完整信息建立不了，此时就要用第三类方法来建立hash了
 *         </li>
 *     </ol>
 * </p>
 * <p>DoubleArrayTrie 是属于第二类hash,而Trie是属于第三类hash。DoubleArrayTrie的仅有的复杂性完全体现在定义一一对应上（实际就是解决
 * base和check的冲突上)，也就是建立对象同自然数之间的一一对应同时又使空间占用少</p>
 */
public final class Trie<T> {
    /**
     * 虚根节点
     */
    final TrieNode<T> mRootTrieNode = new TrieNode<T>( '\0' );
    final int mAverageChildCount;

    public Trie() {
        this( 2 );
    }

    /**
     *
     * @param aAverageChildCount 估计每个节点大概有几个儿子，估计的好的话可以使得构造Trie过程有更少的拷贝数据过程，
     *  甚至完全消除拷贝数据过程使得构造过程更快，但太大的话又有点浪费空间，所以依赖于想更快构建还是使内存占用更少的权衡
     */
    public Trie(int aAverageChildCount) {
        this.mAverageChildCount = aAverageChildCount < 0 ? 1 : aAverageChildCount;
    }

    /**
     * 获取对应节点的数据，如果找不到对应节点或者找到的节点不是数据节点返回null
     */
    public T get(CharSequence aKey) {
        TrieNode<T> searchNode = this.mRootTrieNode;
        for (int i = 0, len = aKey.length(); i < len && searchNode != null; ++i) {
            char nextChar = aKey.charAt( i );
            int childNodeIndex = searchNode.binarySearchChildNodeIndex( nextChar );
            searchNode = childNodeIndex >= 0 ? searchNode.mChildrenNodes[ childNodeIndex ] : null;
        }
        //如果是空串就返回虚根节点上的value
        return searchNode != null ? searchNode.mValue : null;
    }

    /**
     * 询问数据是否已经插入
     */
    public boolean containsKey(CharSequence aKey) {
        return this.get( aKey ) != null;
    }

    /**
     * <p>插入数据，若aKey原来已经在Trie树中 ，那么就返回原来的数据并且用新的aValue替换这个原来的数据，否则返回null</p>
     * <p>如果参数aValue为 null, 在功能效果上完全等价于<code>remove(aKey)</code>，不同之处在于：<code>put(aKey,aValue)</code>
     * 查找过程中节点不存在的话就创建节点，也就是蕴含了创建分支的过程</p>
     */
    public T put(CharSequence aKey, T aValue) {
        TrieNode<T> searchNode = this.mRootTrieNode;
        int averageChildCount = this.mAverageChildCount;
        for (int i = 0, keyCharLen = aKey.length(); i < keyCharLen; ++i) {
            char nextChar = aKey.charAt( i );
            searchNode = searchNode.binarySearchChildNodeOrInsertIfNotFound( nextChar, averageChildCount );
        }
        //如果是空串就插入到虚根节点上
        T oldValue = searchNode.mValue;
        searchNode.mValue = aValue;
        return oldValue;
    }

    /**
     * 只有Trie树中aKey原来不在Trie树中或者即使存在但对应的Trie树节点不是数据节点，此时才插入数据。
     * 若原来已经存在数据，本方法不做任何插入动作，仅仅返回原来的值而已。此方法在Trie树构造中很有用，
     * 提供了快速构建的能力。
     * 这实际上在功能上等价于下面的调用：
     * <pre> {@code
     *     if (!trie.containsKey(key))
     *         return trie.put(key, value);
     *     else
     *         return trie.get(key);
     * }</pre>
     * 不同之处在于putIfAbsent实际上只做一次二分搜索就完成put的
     * @param aKey
     * @param aValue
     * @return 原来的value如果原来存在值的话，此时不进行替换依然保留原来的值；或返回null如果原来不存在并绑定新的aValue
     */
    public T putIfAbsent(CharSequence aKey, T aValue) {
        TrieNode<T> searchNode = this.mRootTrieNode;
        int averageChildCount = this.mAverageChildCount;
        for (int i = 0, keyCharLen = aKey.length(); i < keyCharLen; ++i) {
            char nextChar = aKey.charAt( i );
            searchNode = searchNode.binarySearchChildNodeOrInsertIfNotFound( nextChar, averageChildCount );
        }
        //如果是空串就插入到虚根节点上
        T oldValue = searchNode.mValue;
        if (oldValue == null) {
            searchNode.mValue = aValue;
        }
        return oldValue;
    }

    /**
     * 删除对应节点上的数据，注意该节点和分支依然保留，仅仅移除的是节点与数据的绑定关系而已，
     * 因此删除速度很快，因为不用重新整理Trie树结构的嘛，而且以后如果在相同节点上建立数据也很快。
     * 缺点是造成一点空间浪费，不过一般用Trie树也是临时构建用途（边构造边检查），即使临时浪费一点也没有什么，
     * 因此如果想长久使用，应该在数据构建完毕后转成DAT来静态使用。
     */
    public T remove(CharSequence aKey) {
        int keyCharLen = aKey.length();
        TrieNode<T> parentNode = null;
        TrieNode<T> childNode = this.mRootTrieNode;
        int childNodeIndex = -1;
        for (int i = 0; i < keyCharLen; ++i) {
            char nextChar = aKey.charAt( i );
            parentNode = childNode;
            childNodeIndex = parentNode.binarySearchChildNodeIndex( nextChar );
            if (childNodeIndex >= 0) {
                childNode = parentNode.mChildrenNodes[ childNodeIndex ];
            }
            else {
                break;
            }
        }
        return childNodeIndex >= 0 ? childNode.removeValue() : (keyCharLen == 0 ? this.mRootTrieNode.removeValue() : null);
    }

    /**
     * 删除整个Trie树分支的数据，每删除一个数据，就通过aHit回调。注意，如果aKey是空串，实际表示删除整棵Trie树。
     * 删除的结果是分支根节点依然保留在Trie中但此分支根节点以下被清空了
     */
    public void removeBranch(CharSequence aBranchRootKey, Hit<T> aHit) {
        int keyCharLen = aBranchRootKey.length();
        TrieNode<T> parentNode = null;
        TrieNode<T> branchNode = this.mRootTrieNode;
        int childNodeIndex = -1;
        for (int i = 0; i < keyCharLen; ++i) {//先找到对应的分支根节点
            char nextChar = aBranchRootKey.charAt( i );
            parentNode = branchNode;
            childNodeIndex = parentNode.binarySearchChildNodeIndex( nextChar );
            if (childNodeIndex >= 0) {
                branchNode = parentNode.mChildrenNodes[ childNodeIndex ];
            }
            else {
                break;
            }
        }
        branchNode = childNodeIndex >= 0 ? branchNode : (keyCharLen == 0 ? this.mRootTrieNode : null);
        if (branchNode != null) {
            //匹配到分支了
            StringBuilder keyBuffer = new StringBuilder( keyCharLen + 1 );
            keyBuffer.append( aBranchRootKey );
            branchNode.removeBranchValue( keyBuffer, aHit );
        }
    }

    /**
     * 转换成双数组DAT树来使用，推荐应该在Trie完全构造完毕不再变化后调用此方法来转成DAT使用，
     * 返回的DAT是Trie数据的此时刻的一个镜像，意味着后续对Trie树的改变不会传导给已经构造的DAT，
     */
    public DoubleArrayTrie<T> toDoubleArrayTrie() {
        return DoubleArrayTrieMaker.convert( this );
    }

    static final class TrieNode<T> {
        final char mChar;
        T mValue;//该节点上绑定的数据，如果为null就表示此节点非数据节点，但以此节点为根的分支依然可能有其他数据

        /**
         * 本来可以直接使用ArrayList<TrieNode<T>>简化的，但为了二分查找的时候避免频繁地调用get(i),
         * 因此这里干脆直接操纵数组算求，反而更jer直接简单
         */
        TrieNode<T> [] mChildrenNodes;//mChildrenNodes.length 仅仅表示容量大小而不是儿子节点个数
        int mChildCount;//儿子节点个数: mChildCount <= mChildrenNodes.length

        TrieNode(char aChar) {
            this.mChar = aChar;
        }

        int binarySearchChildNodeIndex(char aMatchChildChar) {
            TrieNode<T> [] childrenNodes = this.mChildrenNodes;
            if (childrenNodes == null) {
                return -1;
            }
            int low = 0;
            int high = this.mChildCount - 1;//下标索引闭区间[low, high]
            int mid = (low + high) >>> 1;//避免除以2
            char midChildChar = childrenNodes[ mid ].mChar;
            while (low < high) {
                if (midChildChar == aMatchChildChar) {
                    break;
                }
                else {
                    if (midChildChar < aMatchChildChar) {
                        low = mid + 1;
                    }
                    else {
                        high = mid - 1;
                    }
                    mid = (low + high) >>> 1;//避免除以2
                    midChildChar = childrenNodes[ mid ].mChar;
                }
            }
            return midChildChar == aMatchChildChar ? mid : (midChildChar < aMatchChildChar ? -mid - 2 : -mid - 1);
        }

        @SuppressWarnings("unchecked")
        TrieNode<T> binarySearchChildNodeOrInsertIfNotFound(char aMatchChildChar, int aAverageChildCount) {
            int childNodeIndex = this.binarySearchChildNodeIndex( aMatchChildChar );
            if (childNodeIndex >= 0) {
                return this.mChildrenNodes[ childNodeIndex ];
            }
            else {
                TrieNode<T> newChildNode = new TrieNode<T>( aMatchChildChar );
                TrieNode<T> [] childrenNodes = this.mChildrenNodes;
                if (childrenNodes == null) {
                    childrenNodes = new TrieNode [ aAverageChildCount ];
                    childrenNodes[ this.mChildCount++ ] = newChildNode;//0位置插入
                    this.mChildrenNodes = childrenNodes;
                }
                else {
                    int posInsertingAt = -childNodeIndex - 1;
                    if (this.mChildCount == childrenNodes.length) {
                        childrenNodes = Arrays.copyOf( childrenNodes, childrenNodes.length + aAverageChildCount );
                        this.mChildrenNodes = childrenNodes;
                    }
                    System.arraycopy( childrenNodes, posInsertingAt, childrenNodes, posInsertingAt + 1, this.mChildCount++ - posInsertingAt );
                    childrenNodes[ posInsertingAt ] = newChildNode;//此位置插入Trie树节点
                }
                return newChildNode;
            }
        }

        T removeValue() {
            T oldValue = this.mValue;
            this.mValue = null;//解除数据绑定关系，树节点依然保留
            return oldValue;
        }

        boolean removeBranchValue(StringBuilder aKeyBuffer, Hit<T> aHit) {
            T oldValue = this.mValue;
            this.mValue = null;//解除数据绑定关系
            boolean whetherContinueHit = aHit.hit( aKeyBuffer, 0, aKeyBuffer.length(), oldValue );
            if (whetherContinueHit) {
                TrieNode<T> [] childrenNodes = this.mChildrenNodes;
                int childCount = this.mChildCount = 0;
                this.mChildrenNodes = null;//清空分支以下
                this.mChildCount = 0;

                return childCount > 0 ? Arrays.stream( childrenNodes, 0, childCount ).allMatch( (aChildNode) -> {
                    aKeyBuffer.append( aChildNode.mChar );
                    boolean whetherContinueHitBrothers = aChildNode.removeBranchValue( aKeyBuffer, aHit );
                    aKeyBuffer.setLength( aKeyBuffer.length() - 1 );
                    return whetherContinueHitBrothers;
                } ) : true;
            }
            else {
                return false;
            }
        }
    }
}
