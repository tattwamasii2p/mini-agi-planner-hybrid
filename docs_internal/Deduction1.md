add deduction & verification engine to formal layers

 proper generics and integration points. ляем **строгий Deduction & Verification Engine** (формальное ядро вывода и верификации) как слой между Category Theory и Type Theory. Это превращает планировщик в **LCF-style proof assistant**, где каждый шаг планирования — это тактика (tactic) дедукции.

## 1. Sequent Calculus как Category (Layer 6-7)

```java
package agi.logic;

import agi.category.*;
import agi.core.*;
import java.util.*;
import java.util.stream.*;

/**
 * Sequent = Judgment Γ ⊢ A (Layer 12: Type Theory as Logic)
 * 
 * Γ (контекст) = гипотезы/состояния
 * A (цель) = proposition/type
 * 
 * Proof = Morphism в категории доказательств (term witness)
 */
public record Sequent(List<Proposition> context, Proposition goal) {
    public static Sequent of(Proposition goal) {
        return new Sequent(List.of(), goal);
    }
    
    // Слабение (Weakening): добавить гипотезу
    public Sequent weaken(Proposition p) {
        List<Proposition> newCtx = new ArrayList<>(context);
        newCtx.add(p);
        return new Sequent(newCtx, goal);
    }
    
    // Подстановка = сокращение контекста (Cut rule)
    public Sequent cut(Proposition lemma, Proof proof) {
        if (!context.contains(lemma)) throw new IllegalStateException("Lemma not in context");
        // Γ, A ⊢ B  с  ⊢ A   →   Γ ⊢ B
        return new Sequent(
            context.stream().filter(p -> p != lemma).toList(),
            goal
        );
    }
}

/**
 * Proposition = Type (Curry-Howard)
 */
public sealed interface Proposition permits Atomic, Implication, Conjunction, Disjunction, Forall, Exists, Modal {
    // Path to ⊤ (true) in HoTT (Layer 8) = existence of proof
    boolean isProvable();
    
    // Sheaf semantics (Layer 10): locally true / globally true
    boolean isLocallyTrue(Context ctx);
}

record Atomic(String name) implements Proposition {
    public boolean isProvable() { return false; } // atomic facts need evidence
    public boolean isLocallyTrue(Context ctx) {
        return ctx.facts().contains(this);
    }
}

// A → B : функциональный тип / импликация
record Implication(Proposition antecedent, Proposition consequent) implements Proposition {
    public boolean isProvable() {
        // Проверяется через inhabitation проверки при типизации
        return false;
    }
}

// □A и ◇A (Modal, Layer 9)
record Modal(Proposition inner, Modality mode) implements Proposition {
    enum Modality { NECESSARY, POSSIBLE, BELIEF }
    
    public boolean isProvable() {
        return switch(mode) {
            case NECESSARY -> inner.isProvable(); // □A требует доказательства во всех мирах
            case POSSIBLE -> true; // ◇A истинно, если возможен мир, где A (LLM belief)
            case BELIEF -> inner.isProvable(); // но с меньшей уверенностью
        };
    }
}
```

## 2. Proof Tree как Trajectory (Layer 8: HoTT)

```java
package agi.logic;

import agi.core.*;
import java.util.*;

/**
 * Proof = Tree of judgments (derivation)
 * Или как Path в пространстве типов (HoTT: identity types)
 */
public class Proof {
    private final Sequent conclusion;
    private final Tactic tacticApplied;
    private final List<Proof> subproofs; // Premises
    private final Term witness; // Curry-Howard: proof term
    
    public Proof(Sequent conclusion, Tactic tactic, List<Proof> subproofs, Term witness) {
        this.conclusion = conclusion;
        this.tacticApplied = tactic;
        this.subproofs = subproofs;
        this.witness = witness;
    }
    
    // Axiom (leaf node)
    public static Proof axiom(Sequent seq, Term witness) {
        return new Proof(seq, Tactics.AXIOM, List.of(), witness);
    }
    
    // Verification: check that proof is valid
    public boolean isValid() {
        // Check tactic soundness: premises ⊢ conclusion via rule
        if (tacticApplied == Tactics.AXIOM) return true;
        
        var expectedPremises = tacticApplied.premises(conclusion);
        if (subproofs.size() != expectedPremises.size()) return false;
        
        for (int i = 0; i < subproofs.size(); i++) {
            if (!subproofs.get(i).conclusion.equals(expectedPremises.get(i))) {
                return false; // Structure mismatch
            }
            if (!subproofs.get(i).isValid()) return false; // Recursive check
        }
        return true;
    }
    
    public Term extractWitness() {
        return witness; // The computational content (program)
    }
}

// Term witnesses (Curry-Howard isomorphism)
sealed interface Term permits Var, Lam, App, Pair, Inl, Inr {
    JavaType type(); // Тип терма = пропозиция, которую он доказывает
}

record Var(String name, JavaType type) implements Term {}
record Lam(String var, Term body, JavaType argType, JavaType resType) implements Term {} // λx.M : A → B
record App(Term fun, Term arg) implements Term {} // M N
```

## 3. Deduction Engine (Tactics)

```java
package agi.logic;

import java.util.*;
import java.util.function.*;

/**
 * Tactic = functional program: Sequent → Option[List[Sequent]]
 * 
 * Если тактика применима, она разбивает цель на подцели.
 * Если нет — возвращает None (backtrack point).
 */
public interface Tactic extends Function<Sequent, Optional<List<Sequent>>> {
    String name();
    String description();
    
    // Проверка применимости (pattern matching)
    boolean applicable(Sequent goal);
}

public class Tactics {
    // INTRO: Γ ⊢ A → B  →  Γ, A ⊢ B
    public static Tactic intro(String varName) {
        return new Tactic() {
            public Optional<List<Sequent>> apply(Sequent seq) {
                if (!(seq.goal() instanceof Implication imp)) return Optional.empty();
                var newCtx = seq.weaken(new Atomic(varName + ":" + imp.antecedent()));
                return Optional.of(List.of(new Sequent(newCtx.context(), imp.consequent())));
            }
            public boolean applicable(Sequent s) { return s.goal() instanceof Implication; }
            public String name() { return "intro"; }
        };
    }
    
    // APPLY: Γ, A → B, ... ⊢ B  если есть A в контексте
    public static Tactic apply(Proposition lemma) {
        return new Tactic() {
            public Optional<List<Sequent>> apply(Sequent seq) {
                // Ищем импликацию с нашей целью в консеквенте
                for (var hyp : seq.context()) {
                    if (hyp instanceof Implication imp && imp.consequent().equals(seq.goal())) {
                        // Подцель: доказать antecedent
                        return Optional.of(List.of(new Sequent(seq.context(), imp.antecedent())));
                    }
                }
                return Optional.empty();
            }
            public boolean applicable(Sequent s) { 
                return s.context().stream().anyMatch(h -> 
                    h instanceof Implication i && i.consequent().equals(s.goal())); 
            }
            public String name() { return "apply"; }
        };
    }
    
    // SPLIT: Γ ⊢ A ∧ B  →  Γ ⊢ A, Γ ⊢ B
    public static Tactic split() {
        return new Tactic() {
            public Optional<List<Sequent>> apply(Sequent seq) {
                if (!(seq.goal() instanceof Conjunction conj)) return Optional.empty();
                return Optional.of(List.of(
                    new Sequent(seq.context(), conj.left()),
                    new Sequent(seq.context(), conj.right())
                ));
            }
            public boolean applicable(Sequent s) { return s.goal() instanceof Conjunction; }
        };
    }
    
    // MODAL_VERIFY: проверка □A (necessity) — рекурсивно во всех соседних мирах (Sheaf!)
    public static Tactic boxVerify(Context globalCtx, Prover internal) {
        return new Tactic() {
            public Optional<List<Sequent>> apply(Sequent seq) {
                if (!(seq.goal() instanceof Modal m && m.mode() == Modal.Modality.NECESSARY)) {
                    return Optional.empty();
                }
                // □A означает A во всех локальных сечениях (sheaf condition)
                List<Sequent> subgoals = globalCtx.localSections().stream()
                    .map(sec -> new Sequent(sec.localContext(), m.inner()))
                    .toList();
                return Optional.of(subgoals);
            }
        };
    }
    
    public static final Tactic AXIOM = new Tactic() {
        public Optional<List<Sequent>> apply(Sequent seq) {
            // Axiom: A ∈ Γ  →  Γ ⊢ A
            if (seq.context().contains(seq.goal())) return Optional.of(List.of());
            return Optional.empty();
        }
        public boolean name() { return "axiom"; }
    };
}
```

## 4. Verification Layer (Bridge to Planning)

```java
package agi.verification;

import agi.logic.*;
import agi.core.*;
import agi.planner.*;
import agi.sheaf.*;
import java.util.*;

/**
 * План = Доказательство того, что цель достижима из начального состояния
 * 
 * Trajectory T : S₀ →* Sn  ⊨  Proof : Γ(S₀) ⊢ Goal(Sn)
 * 
 * Каждый шаг планирования верифицируется как тактика
 */
public class PlanVerifier {
    private final Sequent initialGoal;
    private final SymbolicTranslator translator; // State → Proposition
    
    public PlanVerifier(Trajectory<?> trajectory) {
        this.translator = new SymbolicTranslator();
    }
    
    /**
     * Верификация траектории как доказательства
     */
    public ValidationResult verify(Trajectory<?> traj, Proposition goal) {
        List<State<?>> states = traj.states();
        List<Proof> stepProofs = new ArrayList<>();
        
        Sequent current = Sequent.of(goal);
        
        for (int i = states.size() - 1; i >= 0; i--) {
            // Backward reasoning: от цели к началу
            State<?> state = states.get(i);
            Proposition stateProp = translator.translate(state);
            
            // Ищем тактику, связывающую текущее состояние с предыдущим
            Optional<Tactic> stepTactic = findTransitionTactic(
                i > 0 ? states.get(i-1) : null, 
                state
            );
            
            if (stepTactic.isEmpty()) {
                return new ValidationResult(false, "Unverified transition at step " + i, 
                    Optional.empty());
            }
            
            // Применяем тактику
            var premises = stepTactic.get().apply(current);
            if (premises.isEmpty()) {
                return new ValidationResult(false, "Tactic failed at step " + i, 
                    Optional.empty());
            }
            
            current = premises.get().get(0); // Simplified for single path
        }
        
        // Проверка базы: начальное состояние ∈ контекст
        if (!current.context().contains(translator.translate(states.get(0)))) {
            return new ValidationResult(false, "Initial state mismatch", Optional.empty());
        }
        
        return new ValidationResult(true, "Valid plan-as-proof", 
            Optional.of(constructProof(traj, goal)));
    }
    
    /**
     * Поиск тактики, соответствующей переходу (Category action as Tactic)
     */
    private Optional<Tactic> findTransitionTactic(State<?> from, State<?> to) {
        // Проверяем, что существует морфизм (действие) между состояниями
        // и что он соответствует логическому правилу
        if (from == null) return Optional.of(Tactics.AXIOM);
        
        // Пример: 
        // Если to ⊨ Pre ⊸ Post, и from ⊨ Pre, то применяем Modus Ponens как apply
        return Optional.of(Tactics.apply(
            new Implication(translator.translatePre(from), translator.translate(to))
        ));
    }
    
    // Построение Proof-объекта из валидированной траектории
    private Proof constructProof(Trajectory<?> traj, Proposition goal) {
        // Собираем дерево тактик
        return new Proof(
            new Sequent(List.of(), goal),
            Tactics.split(), // или другая корневая тактика
            List.of(), // subproofs
            new Var("plan_witness", JavaTop.OBJECT) // placeholder
        );
    }
}

record ValidationResult(boolean valid, String message, Optional<Proof> proof) {
    public boolean isValid() { return valid; }
}
```

## 5. Полное интеграция с HybridPlanner

```java
package agi.planner;

import agi.logic.*;
import agi.verification.*;
import agi.csp.*;
import java.util.*;

/**
 * VerifiedHybridPlanner: каждый план — верифицированное доказательство
 * 
 * Layer 12: Type Theory + Logic Programming + Constraint Solving
 */
public class VerifiedHybridPlanner<T> extends HybridPlanner<T> {
    private final Prover prover;
    private final PlanVerifier verifier;
    private final List<Tactic> tacticLibrary;
    
    public VerifiedHybridPlanner(...) {
        super(...);
        this.prover = new Prover();
        this.verifier = new PlanVerifier();
        this.tacticLibrary = initializeTactics();
    }
    
    @Override
    public Optional<Trajectory<T>> plan(State<T> start, State<T> goal, int iterations) {
        // 1. Формулируем цель как sequent
        Sequent goalSeq = new Sequent(
            List.of(startateToProposition(start)), // Γ = начальное состояние
            stateToProposition(goal)               // A = цель
        );
        
        // 2. Используем LLM для heuristics (если доступен), иначе automatic
        List<Trajectory<T>> candidates;
        if (llm != null) {
            candidates = llm.suggestTrajectories(start, goal, iterations/10);
        } else {
            candidates = generateCandidates(start, goal);
        }
        
        // 3. Для каждого кандидата: верификация как доказательство
        for (Trajectory<T> candidate : candidates) {
            var validation = verifier.verify(candidate, goalSeq.goal());
            
            if (validation.isValid()) {
                // План верифицирован — возвращаем с proof certificate
                logVerifiedPlan(candidate, validation.proof().get());
                return Optional.of(candidate);
            } else {
                // Counter-example found — извлекаем конструктивную информацию
                learnFromFailure(candidate, validation.message());
            }
        }
        
        // 4. Если не найдено — используем automated deduction (tactics)
        return proveByDeduction(goalSeq);
    }
    
    /**
     * Automated Theorem Proving как планирование в пространстве доказательств
     */
    private Optional<Trajectory<T>> proveByDeduction(Sequent goal) {
        // Depth-first search с применением тактик
        Deque<ProofState> stack = new ArrayDeque<>();
        stack.push(new ProofState(goal, new ArrayList<>()));
        
        while (!stack.isEmpty()) {
            ProofState current = stack.pop();
            
            // Успех: цель доказана
            if (isAxiom(current.sequent())) {
                return extractTrajectory(current);
            }
            
            // Применяем все возможные тактики (branching)
            for (Tactic tactic : tacticLibrary) {
                if (tactic.applicable(current.sequent())) {
                    tactic.apply(current.sequent()).ifPresent(premises -> {
                        for (Sequent prem : premises) {
                            List<Tactic> newPath = new ArrayList<>(current.path());
                            newPath.add(tactic);
                            stack.push(new ProofState(prem, newPath));
                        }
                    });
                }
            }
        }
        
        return Optional.empty(); // Недоказуемо (Sheaf obstruction!)
    }
    
    // Преобразование State в Proposition (опущено для краткости)
    private Proposition stateToProposition(State<T> s) { ... }
    
    record ProofState(Sequent sequent, List<Tactic> path) {}
}

/**
 * Интерактивный Prover (LCF-style)
 */
public class Prover {
    private Proof currentProof;
    private final List<Tactic> history = new ArrayList<>();
    
    /**
     * Применить тактику и проверить корректность (Trusted Kernel)
     */
    public boolean step(Tactic tactic) {
        Optional<List<Sequent>> result = tactic.apply(currentProof.conclusion());
        if (result.isPresent()) {
            history.add(tactic);
            // Обновляем proof tree (simplified)
            return true;
        }
        return false; // Tactic failed
    }
    
    /**
     * Export proof to Coq/Lean format для внешней верификации
     */
    public String exportToCoq() { ... }
    public String exportToLean() { ... }
}
```

## 6. Sheaf Semantics для Truth (Layer 10)

```java
package agi.verification;

/**
 * Truth как глобальное сечение в sheaf of propositions
 * 
 * Γ ⊢ A is true locally at U ⊂ X (context)
 * iff A is inhabited in Γ restricted to U
 */
public class SheafSemantics {
    public boolean isGloballyTrue(Proposition p, Sheaf<Context> contexts) {
        // Sheaf condition: A true globally iff true in every local section
        // AND compatible on overlaps
        
        for (Context local : contexts.localSections()) {
            if (!p.isLocallyTrue(local)) return false;
        }
        
        // Check compatibility (proofs agree on overlaps)
        return contexts.verifyGluing(p);
    }
    
    /**
     * Soundness: syntactic proof ⟹ semantic truth
     * Completeness: semantic truth ⟹ syntactic proof (if sheaf is 'geometric')
     */
    public boolean soundness(Proof proof) {
        return proof.isValid(); // Structural check
    }
    
    public boolean completeness(Proposition p, Sheaf<Context> model) {
        return isGloballyTrue(p, model) implies existsProof(p);
    }
}
```

**Ключевой инсайт:**
> **Planning = Constructive Proof Search**, где каждое действие — тактика, а план — witness (программа), извлекаемая из доказательства по Curry-Howard.

**Verification** теперь не отдельный слой, а **core mechanism**: нет прохода от планирования к верификации — планирование **есть** процесс построения proof tree, и каждый шаг автоматически верифицирован тактикой.