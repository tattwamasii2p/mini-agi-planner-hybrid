package com.adam.agri.planner.logic;

import java.util.List;
import java.util.Optional;

/**
 * Standard tactics library - public access for external packages.
 */
public final class Tactics {

    /**
     * Axiom: goal in context → done.
     */
    public static final Tactic AXIOM = new Tactic() {
        @Override
        public Optional<List<Sequent>> apply(Sequent seq) {
            if (seq.context().contains(seq.goal())) {
                return Optional.of(List.of());
            }
            return Optional.empty();
        }

        @Override
        public boolean applicable(Sequent s) {
            return s.context().contains(s.goal());
        }

        @Override
        public String name() {
            return "axiom";
        }
    };

    /**
     * Unknown/incomplete tactic.
     */
    public static final Tactic UNKNOWN = new Tactic() {
        @Override
        public Optional<List<Sequent>> apply(Sequent seq) {
            return Optional.empty();
        }

        @Override
        public boolean applicable(Sequent s) {
            return false;
        }

        @Override
        public String name() {
            return "unknown";
        }
    };

    /**
     * Intro: Γ ⊢ A → B → Γ, A ⊢ B
     * Introduce assumption, prove consequent.
     */
    public static Tactic intro(String varName) {
        return new Tactic() {
            @Override
            public Optional<List<Sequent>> apply(Sequent seq) {
                if (!(seq.goal() instanceof Implication imp)) {
                    return Optional.empty();
                }

                Proposition assumption = new Atomic(varName + ":" + imp.antecedent());
                Sequent newSeq = seq.weaken(assumption);
                return Optional.of(List.of(
                    new Sequent(newSeq.context(), imp.consequent())
                ));
            }

            @Override
            public boolean applicable(Sequent s) {
                return s.goal() instanceof Implication;
            }

            @Override
            public String name() {
                return "intro";
            }
        };
    }

    /**
     * Apply/Modus Ponens: use implication from context.
     */
    public static Tactic apply(Proposition lemma) {
        return new Tactic() {
            @Override
            public Optional<List<Sequent>> apply(Sequent seq) {
                // Find implication with our goal as consequent
                for (Proposition hyp : seq.context()) {
                    if (hyp instanceof Implication imp &&
                        imp.consequent().equals(seq.goal())) {
                        // Subgoal: prove antecedent
                        return Optional.of(List.of(
                            new Sequent(seq.context(), imp.antecedent())
                        ));
                    }
                }
                return Optional.empty();
            }

            @Override
            public boolean applicable(Sequent s) {
                return s.context().stream().anyMatch(h ->
                    h instanceof Implication imp &&
                    imp.consequent().equals(s.goal())
                );
            }

            @Override
            public String name() {
                return "apply";
            }
        };
    }

    /**
     * Apply generic: find any matching implication.
     */
    public static Tactic apply() {
        return new Tactic() {
            @Override
            public Optional<List<Sequent>> apply(Sequent seq) {
                for (Proposition hyp : seq.context()) {
                    if (hyp instanceof Implication imp &&
                        imp.consequent().equals(seq.goal())) {
                        return Optional.of(List.of(
                            new Sequent(seq.context(), imp.antecedent())
                        ));
                    }
                }
                return Optional.empty();
            }

            @Override
            public boolean applicable(Sequent s) {
                return s.context().stream().anyMatch(h ->
                    h instanceof Implication imp &&
                    imp.consequent().equals(s.goal())
                );
            }

            @Override
            public String name() {
                return "apply";
            }
        };
    }

    /**
     * Split: Γ ⊢ A ∧ B → [Γ ⊢ A, Γ ⊢ B]
     */
    public static Tactic split() {
        return new Tactic() {
            @Override
            public Optional<List<Sequent>> apply(Sequent seq) {
                if (!(seq.goal() instanceof Conjunction conj)) {
                    return Optional.empty();
                }
                return Optional.of(List.of(
                    new Sequent(seq.context(), conj.left()),
                    new Sequent(seq.context(), conj.right())
                ));
            }

            @Override
            public boolean applicable(Sequent s) {
                return s.goal() instanceof Conjunction;
            }

            @Override
            public String name() {
                return "split";
            }
        };
    }

    /**
     * Left injection: Γ ⊢ A ∨ B from Γ ⊢ A.
     */
    public static Tactic left(Proposition right) {
        return new Tactic() {
            @Override
            public Optional<List<Sequent>> apply(Sequent seq) {
                if (!(seq.goal() instanceof Disjunction)) {
                    return Optional.empty();
                }
                // Current goal should be left
                return Optional.of(List.of(
                    new Sequent(seq.context(), seq.goal())
                ));
            }

            @Override
            public boolean applicable(Sequent s) {
                return s.goal() instanceof Disjunction;
            }

            @Override
            public String name() {
                return "left";
            }
        };
    }

    /**
     * Weaken: add proposition to context.
     */
    public static Tactic weaken(Proposition p) {
        return new Tactic() {
            @Override
            public Optional<List<Sequent>> apply(Sequent seq) {
                return Optional.of(List.of(seq.weaken(p)));
            }

            @Override
            public boolean applicable(Sequent s) {
                return true;
            }

            @Override
            public String name() {
                return "weaken";
            }
        };
    }

    /**
     * Modal verify: check □A (necessity) in all local sections.
     * Sheaf condition verification.
     */
    public static Tactic boxVerify(Context globalCtx) {
        return new Tactic() {
            @Override
            public Optional<List<Sequent>> apply(Sequent seq) {
                if (!(seq.goal() instanceof Modal m &&
                      m.mode() == Modal.Modality.NECESSARY)) {
                    return Optional.empty();
                }

                // □A means A in all local sections
                // For now: simplified check
                return Optional.of(List.of(
                    new Sequent(seq.context(), m.inner())
                ));
            }

            @Override
            public boolean applicable(Sequent s) {
                return s.goal() instanceof Modal m &&
                    m.mode() == Modal.Modality.NECESSARY;
            }

            @Override
            public String name() {
                return "box_verify";
            }
        };
    }

    /**
     * Assume: add hypothesis and continue.
     */
    public static Tactic assume(String name, Proposition prop) {
        return new Tactic() {
            @Override
            public Optional<List<Sequent>> apply(Sequent seq) {
                Sequent weakened = seq.weaken(new Atomic(name + ":" + prop));
                return Optional.of(List.of(weakened));
            }

            @Override
            public boolean applicable(Sequent s) {
                return true;
            }

            @Override
            public String name() {
                return "assume";
            }
        };
    }
}