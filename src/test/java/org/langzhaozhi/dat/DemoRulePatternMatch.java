package org.langzhaozhi.dat;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>演示在规则模式匹配中的应用</p>
 * <p>问题源于大量的url匹配需求，每个url表达式有一定的先后规则次序，例如必须先出现abc，接着defghi,  最后xyz</p>
 * <p>实现了一个<规则处理机>RuleMachine来管理这些规则，并提供AC自动机模式匹配服务，它是不变对象，
 * 因此可任意被多线程并发地处理各种模糊字符串的模式匹配，只是每个线程必须通过createRuleStateSpace()来隔离自己的内部状态变迁</p>
 *
 * <p>也可以看到，同前面很多例子一样，多线程并发程序可以从来不用synchronized关键字，以及java.util.concurrent.locks下的各种锁，
 * 原因在于只要做到有意识地隔离不变对象（可并发共享）和可变对象（不可并发共享）就可以很容易做到这点：前者可以看成无状态对象
 * （包括一旦创建或初始化完毕就不可更改内部状态的对象）,当然可随意多线程共享了；而后者的内部状态是可变的，只要隔离到单独线程中使用就行了
 * </p>
 * <p>进一步明确化定义，下面每个概念都对应一个类：
 * <ol>
 *     <li><规则处理机>RuleMache：不变对象</li>
 *     <li><规则>Rule：不变对象</li>
 *     <li><规则命中模式>RuleHitPattern：不变对象，定义了一条规则所期望的词汇在匹配过程中依次命中的顺序序列</li>
 *     <li><规则状态空间>RuleStateSpace：可变对象</li>
 * </ol>
 * </p>
 * <p>此Demo本人langzhaozhi非常喜爱，真正演示了双数组AC自动机在模式表达式匹配中的强大威力</p>
 */
public class DemoRulePatternMatch {
    public static void main(String [] args) {
        //规则表达式，这里只是简单以*进行模式表达式的分割后提取词汇表，注意后面每条规则采用这里的数组下标来标示其唯一性了
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
        RuleMachine ruleMachine = RuleMachine.makeRuleMachine( ruleExpressions );

        //OK,进过上述5个步骤准备工作就绪，就可以进行下面的快速模式匹配了
        String [] fuzzyExpressions = {
            "8g8fgjava0i84cv296fraddftha8o6pythoni8002ffdfyawec94lclojour3d4aabc21gjava0o9fjysyd2", "90ftqg[javaiwwgbcdecrruuwdedsfafwdilmnerwebcdefwir875sud7rf3hhexyzotr8ffkfy34d3yewdroisi53r3",
        };

        //先看下结果
        DemoRulePatternMatch.runDemoResult( ruleMachine, ruleExpressions, fuzzyExpressions );
        //测试速度
        DemoRulePatternMatch.runDemoSpeed( ruleMachine, ruleExpressions, fuzzyExpressions );
    }

    private static void runDemoResult(RuleMachine aRuleMachine, String [] aRuleExpressions, String [] aFuzzyExpressions) {
        RuleStateSpace stateSpaceForThisThread = aRuleMachine.createRuleStateSpace();
        for (int i = 0; i < aFuzzyExpressions.length; ++i) {
            int whichFuzzyExpressionIndex = i;
            String fuzzyExpression = aFuzzyExpressions[ i ];
            stateSpaceForThisThread.matchRule( fuzzyExpression, (aWhichRule) -> {
                System.err.println( "debug:---匹配到第(" + aWhichRule.mRuleID + ")条规则，规则表达式[" + aRuleExpressions[ aWhichRule.mRuleID ] + "], 模糊表达式第(" + whichFuzzyExpressionIndex + ")个[" + fuzzyExpression + "]" );
                return true;
            } );
        }
    }

    private static void runDemoSpeed(RuleMachine aRuleMachine, String [] aRuleExpressions, String [] aFuzzyExpressions) {
        long startTime = System.currentTimeMillis();
        int threadCount = (Runtime.getRuntime().availableProcessors() << 0);//创建(CPU)个并发线程处理
        int loopCount = 100_0000;//测试100万次
        AtomicInteger loopCounter = new AtomicInteger( loopCount );
        for (int i = 0; i < threadCount; ++i) {
            new Thread( () -> {
                //各并发线程共享同一个<规则处理机>，但每个线程必须创建自己独立的<规则状态空间>来运行自己的问题域
                RuleStateSpace stateSpaceForThisThread = aRuleMachine.createRuleStateSpace();
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

    static class RuleMachine {
        final Rule [] mRules;
        final DoubleArrayTrieAhoCorasick<Term> mAC;//<DAT-AC自动机>

        private RuleMachine(Rule [] aRules, DoubleArrayTrieAhoCorasick<Term> aAC) {
            this.mRules = aRules;
            this.mAC = aAC;
        }

        static RuleMachine makeRuleMachine(String [] aRuleExpressions) {
            int idGenerator = 0;
            Rule [] ruleArray = new Rule [ aRuleExpressions.length ];
            Trie<Term> termTrie = new Trie<Term>();//动态构建，最后转成DAT-AC自动机
            for (int i = 0; i < aRuleExpressions.length; ++i) {
                //该规则<词汇表>
                String [] ruleVocabulary = Arrays.stream( aRuleExpressions[ i ].split( "\\*" ) ).filter( (aSplitStr) -> aSplitStr.length() > 0 ).toArray( String []::new );
                Term [] ruleHitPatternSequence = new Term [ ruleVocabulary.length ];
                Term nextNewTerm = new Term( idGenerator, i );
                for (int j = 0; j < ruleVocabulary.length; ++j) {
                    String nextTermText = ruleVocabulary[ j ];
                    Term term = termTrie.putIfAbsent( nextTermText, nextNewTerm );
                    if (term == null) {
                        //说明插入成功
                        term = nextNewTerm;
                        //准备下一个
                        nextNewTerm = new Term( ++idGenerator, i );
                    }
                    else {
                        term.addRuleIndex( i );
                    }
                    ruleHitPatternSequence[ j ] = term;
                }
                RuleHitPattern ruleHitPattern = new RuleHitPattern( ruleHitPatternSequence );
                ruleArray[ i ] = new Rule( i, ruleHitPattern );

                //打印看下呢
                System.err.print( "debug:---第(" + i + ")条规则的<规则命中模式>：#开始#->" );
                Arrays.stream( ruleArray[ i ].mRuleHitPattern.mTermSequence ).forEach( (aTerm) -> System.err.print( aTerm.mTermID + "->" ) );
                System.err.println( "#结束#" );
            }
            return new RuleMachine( ruleArray, termTrie.toDoubleArrayTrie().asAhoCorasick() );
        }

        //为当前调用线程创建一个<规则状态空间>来在此线程中运行自己的问题域
        public RuleStateSpace createRuleStateSpace() {
            return new RuleStateSpace( this );
        }
    }

    static class Rule {
        final int mRuleID;
        final RuleHitPattern mRuleHitPattern;

        Rule(int aRuleID, RuleHitPattern aRuleHitPattern) {
            this.mRuleID = aRuleID;
            this.mRuleHitPattern = aRuleHitPattern;
        }
    }

    static class RuleHitPattern {
        final Term [] mTermSequence;

        RuleHitPattern(Term [] aTermSequence) {
            this.mTermSequence = aTermSequence;
        }
    }

    static class Term {//<词汇>：不变对象
        final int mTermID;//该词汇对应的整数 ID
        int [] mRulesIndex;//该词汇在哪些规则中，因为一个词汇可能在多条规则中被期望命中，因此用数组表示这些不同的规则

        Term(int aID, int aRuleIndex) {
            this.mTermID = aID;
            this.mRulesIndex = new int [] {
                aRuleIndex
            };
        }

        void addRuleIndex(int aNewRuleIndex) {
            if (Arrays.stream( this.mRulesIndex ).allMatch( (aExistIndex) -> aExistIndex != aNewRuleIndex )) {//排除重复
                this.mRulesIndex = Arrays.copyOf( this.mRulesIndex, this.mRulesIndex.length + 1 );
                this.mRulesIndex[ this.mRulesIndex.length - 1 ] = aNewRuleIndex;
            }
        }
    }

    static class RuleStateSpace {//<规则状态空间>：可变对象，在单线程中管理自己的状态迁移
        //private final WeakReference<Thread> mOwnerThreadRef;
        final RuleMachine mOwnerRuleMachine;

        /**
         * 匹配过程中当前每条规则在其<规则命中模式>序列中所期望命中词汇的下标值，体现的是状态迁移
         */
        int [] mCurrentExpectingHitTermIndexInRulePatterns;

        RuleStateSpace(RuleMachine aOwnerRuleMachine) {
            this.mOwnerRuleMachine = aOwnerRuleMachine;
            this.mCurrentExpectingHitTermIndexInRulePatterns = new int [ aOwnerRuleMachine.mRules.length ];
        }

        void matchRule(String aFuzzyExpression, RuleHit aRuleHit) {
            //if (Thread.currentThread() != this.mOwnerThreadRef.get()) {//注释掉算了，外部调用者应该自己保证
            //    throw new Error( "<规则状态空间>必须在自己的线程中运行" );
            //}

            int [] currentExpectingHitTermIndexInRulePatterns = this.mCurrentExpectingHitTermIndexInRulePatterns;
            for (int i = currentExpectingHitTermIndexInRulePatterns.length - 1; i >= 0; --i) {//每次模式匹配前重置
                currentExpectingHitTermIndexInRulePatterns[ i ] = 0;
            }
            this.mOwnerRuleMachine.mAC.matchCaseSensitive( aFuzzyExpression, (aSearchText, aStart, aEnd, aValue) -> {
                //aValue是实际命中的词汇
                //检查该实际命中的词汇在每条规则的<规则命中模式>对应的当前迁移状态（当前期望命中的词汇）是否恰好就是此实际命中的词汇
                for (int ownerRuleIndex : aValue.mRulesIndex) {
                    Rule whichRule = this.mOwnerRuleMachine.mRules[ ownerRuleIndex ];
                    Term [] ruleHitPatternTermSequence = whichRule.mRuleHitPattern.mTermSequence;
                    //该<规则命中模式>在匹配迁移中期望命中的词汇下标
                    int expectingHitTermIndex = currentExpectingHitTermIndexInRulePatterns[ ownerRuleIndex ];
                    if (expectingHitTermIndex < ruleHitPatternTermSequence.length && ruleHitPatternTermSequence[ expectingHitTermIndex ].mTermID == aValue.mTermID) {
                        if (++currentExpectingHitTermIndexInRulePatterns[ ownerRuleIndex ] == ruleHitPatternTermSequence.length) {
                            //匹配到该条规则的<规则命中模式>的每个期望依次命中的词汇都已经实际依次完全匹配成功：该规则被命中了的嘛！
                            boolean whetherContinueHit = aRuleHit.hit( whichRule );
                            if (!whetherContinueHit) {
                                //false表示匹配到第一条规则就停止后面所有的模式匹配过程，否则继续后续模式匹配过程
                                return false;
                            }
                        }
                    }
                }
                return true;
            } );
        }
    }

    @FunctionalInterface
    static interface RuleHit {//规则名中国回调
        public boolean hit(Rule aWhichRule);
    }
}
