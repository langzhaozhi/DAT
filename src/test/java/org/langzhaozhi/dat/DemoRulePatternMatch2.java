package org.langzhaozhi.dat;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>演示在<code>DemoRulePatternMatch</code>上的规则处理的不同处理方式，关键点是：
 * 对词汇hit命中处理时的一个循环 for (int ownerRuleIndex : aValue.mRulesIndex)  的不同处理方式。</p>
 *
 * <p>本例程仅仅适合于存在超大量规则而且很多词汇同时属于各条规则命中模式的情况，此时才能体现本例程价值，否则用前面的
 * <code>DemoRulePatternMatch</code>更简单更好更快，同时也说明管理状态迁移越复杂那么代码编写也越复杂，
 * 有时甚至得不偿失</p>
 *
 * <p>那么，为什么此种条件下这个循环值得改进？举一个极端的例子：假如有5亿条规则，每条规则的词汇命中模式都很长，
 * 而且基本上每个词汇都分布于8千万条规则中，那么此时上面那个循环绝大多数都是无效的，也就是<规则命中模式>
 * 下标在匹配迁移中期望命中的词汇和当前实际hit命中的词汇绝大多数都是不相干的，数据量越大，那么无效占比也越大，
 * 甚至99.99999%的循环内容都是浪费的。为了追求顶巅端的极致点，本Demo就专门针对这种极端特殊情景来设计的，
 * 其实实际用途不大，比起前面<code>DemoRulePatternMatch</code>也是超级复杂</p>
 *
 * <p>前面的概念依然保留，为区分加了一个2后缀，为管理状态方便还额外定义了几个概念：
 * <ol>
 *     <li><规则处理机>RuleMache2：不变对象</li>
 *     <li><规则>Rule2：不变对象</li>
 *     <li><规则命中模式>RuleHitPattern2：不变对象，定义了一条规则所期望的词汇在匹配过程中依次命中的顺序序列</li>
 *     <li><规则状态空间>RuleStateSpace2：可变对象</li>
 *     <li><规则匹配状态>RuleMathcingState2：可变对象</li>
 *     <li><词汇命中状态>TermHitState2：可变对象</li>
 * </ol>
 * </p>
 */
public class DemoRulePatternMatch2 {
    public static void main(String [] args) {
        //规则表达式
        String [] ruleExpressions = {
            "*abc*bcde*lmn*xyzotr*",//这条规则期望依次先命中"abc"后命中"bcde"，再命中"lmn"再命中"xyzotr", 其他雷同
            "*bg*itez*bg*itez*o*", //
            "*java*c*python*clojour*", //
            "*java*c*clojour*lmn*", //
            "*he*his*it*us*uep*zte*", //
            "*c*python*java*", //
            "*bcde*lmn*xyzotr*dr*",//
        };

        //根据规则表达式预先生成一个可并发共享的<规则处理机>
        RuleMachine2 ruleMachine = RuleMachine2.makeRuleMachine( ruleExpressions );

        //要模式匹配的模糊表达式
        String [] fuzzyExpressions = {
            "8g8fgjava0i84cv296fraddftha8o6pythoni8002ffdfyawec94lclojour3d4aabc21gjava0o9fjysyd2", "90ftqg8javaiwwgbcdecrruuwdedsfafwdilmnerwebcdefwir875sud7rf3hhexyzotr8ffkfy34d3yewdroisi53r3",
        };

        //先简单看下结果
        DemoRulePatternMatch2.runDemoResult( ruleMachine, ruleExpressions, fuzzyExpressions );
        //测试速度对比
        DemoRulePatternMatch2.runDemoSpeed( ruleMachine, ruleExpressions, fuzzyExpressions );
    }

    private static void runDemoResult(RuleMachine2 aRuleMachine, String [] aRuleExpressions, String [] aFuzzyExpressions) {
        RuleStateSpace2 stateSpaceForThisThread = aRuleMachine.createRuleStateSpace();
        for (int i = 0; i < aFuzzyExpressions.length; ++i) {
            int whichFuzzyExpressionIndex = i;
            String fuzzyExpression = aFuzzyExpressions[ i ];
            stateSpaceForThisThread.matchRule( fuzzyExpression, (aWhichRule) -> {
                System.err.println( "debug:---匹配到第(" + aWhichRule.mRuleID + ")条规则，规则表达式[" + aRuleExpressions[ aWhichRule.mRuleID ] + "], 模糊表达式第(" + whichFuzzyExpressionIndex + ")个[" + fuzzyExpression + "]" );
                return true;
            } );
        }
    }

    private static void runDemoSpeed(RuleMachine2 aRuleMachine, String [] aRuleExpressions, String [] aFuzzyExpressions) {
        long startTime = System.currentTimeMillis();
        int threadCount = (Runtime.getRuntime().availableProcessors() << 0);//创建(CPU)个并发线程处理
        int loopCount = 100_0000;//测试100万次
        AtomicInteger loopCounter = new AtomicInteger( loopCount );
        for (int i = 0; i < threadCount; ++i) {
            new Thread( () -> {
                //各并发线程共享同一个<规则处理机>，但每个线程必须创建自己独立的<规则状态空间>来运行自己的问题域
                RuleStateSpace2 stateSpaceForThisThread = aRuleMachine.createRuleStateSpace();
                int leftCount = loopCounter.decrementAndGet();
                while (leftCount >= 0) {
                    for (String fuzzyExpression : aFuzzyExpressions) {
                        stateSpaceForThisThread.matchRule( fuzzyExpression, (aWhichRule) -> true );
                    }
                    if (leftCount == 0) {
                        break;
                    }
                    else {
                        leftCount = loopCounter.decrementAndGet();
                    }
                }
                if (leftCount == 0) {
                    //刚好最后一个处理完毕
                    long endTime = System.currentTimeMillis();
                    System.err.println( "debug:---" + loopCount + "次并发匹配花费时间：" + (endTime - startTime) + " ms" );
                }
            } ).start();
        }
    }

    static class RuleMachine2 {
        final Rule2 [] mRules;
        final DoubleArrayTrieAhoCorasick<Integer> mAC;//<DAT-AC自动机>
        final int mTermCount;

        private RuleMachine2(Rule2 [] aRules, DoubleArrayTrieAhoCorasick<Integer> aAC, int aTermCount) {
            this.mRules = aRules;
            this.mAC = aAC;
            this.mTermCount = aTermCount;
        }

        static RuleMachine2 makeRuleMachine(String [] aRuleExpressions) {
            int idGenerator = 0;
            Rule2 [] ruleArray = new Rule2 [ aRuleExpressions.length ];
            Trie<Integer> termTrie = new Trie<Integer>();//动态构建，最后转成DAT-AC自动机
            for (int i = 0, isize = aRuleExpressions.length; i < isize; ++i) {
                String [] ruleVocabulary = Arrays.stream( aRuleExpressions[ i ].split( "\\*" ) ).filter( (aSplitStr) -> aSplitStr.length() > 0 ).toArray( String []::new );
                Integer [] ruleHitPatternSequence = new Integer [ ruleVocabulary.length ];
                Integer newTermID = idGenerator;
                for (int j = 0; j < ruleVocabulary.length; ++j) {
                    String nextTermText = ruleVocabulary[ j ];
                    Integer termID = termTrie.putIfAbsent( nextTermText, newTermID );
                    if (termID == null) {
                        //说明插入成功
                        termID = newTermID;
                        //准备下一个
                        newTermID = ++idGenerator;
                    }
                    ruleHitPatternSequence[ j ] = termID;
                }
                //对每条规则串解析后生成Rule对象
                ruleArray[ i ] = new Rule2( i, new RuleHitPattern2( ruleHitPatternSequence ) );

                //打印看下呢
                System.err.print( "debug:---第(" + i + ")条规则的<规则命中模式>：#开始#->" );
                Arrays.stream( ruleArray[ i ].mRuleHitPattern.mTermSequence ).forEach( (aTermID) -> System.err.print( aTermID + "->" ) );
                System.err.println( "#结束#" );
            }
            //<词汇>总数
            int totalTermCount = idGenerator;
            return new RuleMachine2( ruleArray, termTrie.toDoubleArrayTrie().asAhoCorasick(), totalTermCount );
            //return new RuleMachine2( ruleArray, DoubleArrayTrieMaker.convert( termTrie ).asDoubleArrayTrie().asAhoCorasick(), totalTermCount );
        }

        //为当前调用线程创建一个<规则状态空间>来在此线程中运行自己的问题域
        public RuleStateSpace2 createRuleStateSpace() {
            return new RuleStateSpace2( this );
        }
    }

    static class Rule2 {
        final int mRuleID;
        final RuleHitPattern2 mRuleHitPattern;

        Rule2(int aRuleID, RuleHitPattern2 aRuleHitPattern) {
            this.mRuleID = aRuleID;
            this.mRuleHitPattern = aRuleHitPattern;
        }
    }

    static class RuleHitPattern2 {//<规则命中模式>，静态不变对象，本演示中暂时没有什么用途，为了明确概念而定义出来
        final Integer [] mTermSequence;

        RuleHitPattern2(Integer [] aTermSequence) {
            this.mTermSequence = aTermSequence;
        }
    }

    static class RuleMatchingState2 {//<规则匹配状态>：链表实现
        final Rule2 mOwnerRule;//所属于的<规则>

        /**
         * 该<规则匹配状态>相对于它所属于的<规则>的<规则命中模式>词汇序列中当前期望命中的下标，
         * 实际就是指向 mOwnerRule.mRulePattern.mTermSequence 的下标，初始都是 0
         */
        int mCurrentExpectingHitTermIndexInRulePattern;

        RuleMatchingState2 mNext;//匹配时期望相同<命中词汇状态>其他规则的<规则匹配状态>

        RuleMatchingState2(Rule2 aOwnerRule) {
            this.mOwnerRule = aOwnerRule;
        }

        void reset(RuleStateSpace2 aOwnerSpace) {
            this.mCurrentExpectingHitTermIndexInRulePattern = 0;//初始下标为0，意义为指向<规则命中模式>的词汇序列的第0个
            int firstTermID = this.mOwnerRule.mRuleHitPattern.mTermSequence[ 0 ];//每条<规则>的<规则命中模式>的词汇序列的第0个
            aOwnerSpace.getTermHitState( firstTermID ).addRuleMatchingState( this );
        }

        Integer moveToNextExpectingHitTermID() {
            //该规则对其<规则命中模式>的期望下标值移动到下一位
            return (++this.mCurrentExpectingHitTermIndexInRulePattern == this.mOwnerRule.mRuleHitPattern.mTermSequence.length) ? null : this.mOwnerRule.mRuleHitPattern.mTermSequence[ this.mCurrentExpectingHitTermIndexInRulePattern ];
        }
    }

    static class TermHitState2 {//<词汇命中状态>，同一个<词汇命中状态>可能同时被多个<规则匹配状态>所期望，因此内部用链表管理这些<规则匹配状态>
        RuleMatchingState2 mFirst;//链表头部
        RuleMatchingState2 mLast;
        int mInvocationCounter;

        void reset() {
            this.mFirst = null;
            this.mLast = null;
        }

        void addRuleMatchingState(RuleMatchingState2 aRuleMatchingState) {
            aRuleMatchingState.mNext = null;

            if (this.mFirst == null) {
                //这是第0个的嘛
                this.mFirst = aRuleMatchingState;
            }
            else {
                //其他用链表管理起来
                this.mLast.mNext = aRuleMatchingState;
            }
            this.mLast = aRuleMatchingState;
        }
    }

    static class RuleStateSpace2 {//单线程关联的<规则状态空间>
        private final RuleMachine2 mOwnerRuleMachine;
        final RuleMatchingState2 [] mRuleMatchingStates;
        final TermHitState2 [] mTermHitStates;
        int mInvocationCounter;

        RuleStateSpace2(RuleMachine2 aOwnerRuleMachine) {
            //this.mOwnerThreadRef = new WeakReference<Thread>( Thread.currentThread() );
            this.mOwnerRuleMachine = aOwnerRuleMachine;
            this.mRuleMatchingStates = Arrays.stream( aOwnerRuleMachine.mRules ).map( (aRule) -> new RuleMatchingState2( aRule ) ).toArray( RuleMatchingState2 []::new );
            this.mTermHitStates = new TermHitState2 [ this.mOwnerRuleMachine.mTermCount ];
        }

        TermHitState2 getTermHitState(int aTermID) {//每个termID对应于唯一TermHitState实例，下标映射
            TermHitState2 termHitState = this.mTermHitStates[ aTermID ];
            if (termHitState == null) {
                this.mTermHitStates[ aTermID ] = (termHitState = new TermHitState2());
            }
            if (termHitState.mInvocationCounter != this.mInvocationCounter) {
                termHitState.reset();//说明是前一次matchRule遗留的状态，重置即可
                termHitState.mInvocationCounter = this.mInvocationCounter;
            }
            return termHitState;
        }

        void matchRule(String aFuzzyText, RuleHit2 aRuleHit) {
            //用mInvocationCounter替代每次大循环状态重置,小技巧而已
            if (++this.mInvocationCounter == Integer.MIN_VALUE) {
                //对int 32位循环一遍了，该全部重置下了
                Arrays.stream( this.mTermHitStates ).filter( (aTermHitState) -> aTermHitState != null ).forEach( (aTermHitState) -> aTermHitState.reset() );
            }
            //每次模式匹配之前先初始化每条<规则>的<规则匹配状态>：与其所期望的<规则命中模式>的第一个词汇对应的<词汇命中状态>绑定
            for (RuleMatchingState2 nextRuleMatchingState : this.mRuleMatchingStates) {
                nextRuleMatchingState.reset( this );
            }

            this.mOwnerRuleMachine.mAC.matchCaseSensitive( aFuzzyText, (aSearchText, aStart, aEnd, aValue) -> {
                //aValue是实际命中的词汇的ID
                //对实际命中的词汇在每条规则的<规则命中模式>对应的当前迁移状态进行立即迁移：下标移动到下一个位置
                TermHitState2 termHitState = this.getTermHitState( aValue );
                RuleMatchingState2 nextRuleMatchingState = termHitState.mFirst;

                if (nextRuleMatchingState != null) {
                    termHitState.reset();//必须先重置，为下次命中做初始化，还有就是同一个词汇在同一个规则序列连续的话也必须先重置

                    while (nextRuleMatchingState != null) {
                        RuleMatchingState2 nextNextRuleMatchingState = nextRuleMatchingState.mNext;//必须先记下来,否则其mNext会先被置成null

                        Integer nextExpectingHitTermID = nextRuleMatchingState.moveToNextExpectingHitTermID();
                        if (nextExpectingHitTermID == null) {
                            //匹配到该条规则的<规则命中模式>的每个期望依次命中的词汇都已经实际依次完全匹配成功：该规则被命中了的嘛！
                            boolean whetherContinueHit = aRuleHit.hit( nextRuleMatchingState.mOwnerRule );
                            if (!whetherContinueHit) {
                                //false表示匹配到第一条规则就停止后面所有的模式匹配过程，否则继续后续模式匹配过程
                                return false;
                            }
                        }
                        else {
                            TermHitState2 nextExpectingHitTermState = this.getTermHitState( nextExpectingHitTermID );
                            nextExpectingHitTermState.addRuleMatchingState( nextRuleMatchingState );
                        }
                        nextRuleMatchingState = nextNextRuleMatchingState;
                    }
                }
                return true;
            } );
        }
    }

    @FunctionalInterface
    static interface RuleHit2 {//规则名中国回调
        public boolean hit(Rule2 aWhichRule);
    }
}
