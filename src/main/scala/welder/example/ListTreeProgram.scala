
package welder
package example

import inox._
import inox.trees.{forall => _, _}
import inox.trees.dsl._

object ListTreeProgram {

  val list: Identifier = FreshIdentifier("List")
  val cons: Identifier = FreshIdentifier("Cons")
  val nil: Identifier = FreshIdentifier("Nil")
  val head: Identifier = FreshIdentifier("head")
  val tail: Identifier = FreshIdentifier("tail")

  val tree: Identifier = FreshIdentifier("Tree")
  val branch: Identifier = FreshIdentifier("Branch")
  val leaf: Identifier = FreshIdentifier("Leaf")
  val left: Identifier = FreshIdentifier("left")
  val right: Identifier = FreshIdentifier("right")
  val value: Identifier = FreshIdentifier("value")

  val concatenate: Identifier = FreshIdentifier("concatenate")
  val toList: Identifier = FreshIdentifier("toList")

  val treeMap: Identifier = FreshIdentifier("treeMap")
  val listMap: Identifier = FreshIdentifier("listMap")

  val treeFold: Identifier = FreshIdentifier("treeFold")
  val listFold: Identifier = FreshIdentifier("listFold")

  val listSort = mkSort(list)("A")(Seq(cons, nil))
  val treeSort = mkSort(tree)("A")(Seq(branch, leaf))

  val consConstructor = mkConstructor(cons)("A")(Some(list)) {
    case Seq(tp) =>
      Seq(ValDef(head, tp), ValDef(tail, T(list)(tp)))
  }
  val nilConstructor = mkConstructor(nil)("A")(Some(list))(tps => Seq.empty)
  val branchConstructor = mkConstructor(branch)("A")(Some(tree)) {
    case Seq(tp) =>
      Seq(ValDef(left, T(tree)(tp)), ValDef(right, T(tree)(tp)))
  }
  val leafConstructor = mkConstructor(leaf)("A")(Some(tree)) {
    case Seq(tp) =>
      Seq(ValDef(value, tp))
  }

  val concatenateFunction = mkFunDef(concatenate)("A") { case Seq(tpe) =>
    val args: Seq[ValDef] = Seq("as" :: T(list)(tpe), "bs" :: T(list)(tpe))
    val retType: Type = T(list)(tpe)
    val body: Seq[Variable] => Expr = { case Seq(as, bs) =>
      if_ (as.isInstOf(T(cons)(tpe))) {
        let ("cAs" :: T(cons)(tpe), as.asInstOf(T(cons)(tpe))) { case cAs =>
          T(cons)(tpe)(cAs.getField(head), E(concatenate)(tpe)(cAs.getField(tail), bs))
        }
      } else_ {
        bs
      }
    }

    (args, retType, body)
  }

  val toListFunction = mkFunDef(toList)("A") { case Seq(tpe) =>
    val args: Seq[ValDef] = Seq("t" :: T(tree)(tpe))
    val retType: Type = T(list)(tpe)
    val body: Seq[Variable] => Expr = { case Seq(t) =>
      if_ (t.isInstOf(T(branch)(tpe))) {
        let ("b" :: T(branch)(tpe), t.asInstOf(T(branch)(tpe))) { case b =>
          E(concatenate)(tpe)(
            E(toList)(tpe)(b.getField(left)),
            E(toList)(tpe)(b.getField(right)))
        }
      } else_ {
        T(cons)(tpe)(t.asInstOf(T(leaf)(tpe)).getField(value), T(nil)(tpe)())
      }
    }

    (args, retType, body)
  }

  val treeMapFunction = mkFunDef(treeMap)("A", "B") { case Seq(tpeA, tpeB) =>
    val args: Seq[ValDef] = Seq("t" :: T(tree)(tpeA), "f" :: (tpeA =>: tpeB))
    val retType: Type = T(tree)(tpeB)
    val body: Seq[Variable] => Expr = { case Seq(t, f) =>
      if_ (t.isInstOf(T(branch)(tpeA))) {
        let ("b" :: T(branch)(tpeA), t.asInstOf(T(branch)(tpeA))) { case b =>
          T(branch)(tpeB)(
            E(treeMap)(tpeA, tpeB)(b.getField(left), f),
            E(treeMap)(tpeA, tpeB)(b.getField(right), f))
        }
      } else_ {
        T(leaf)(tpeB)(f(t.asInstOf(T(leaf)(tpeA)).getField(value)))
      }
    }

    (args, retType, body)
  }

  val listMapFunction = mkFunDef(listMap)("A", "B") { case Seq(tpeA, tpeB) =>
    val args: Seq[ValDef] = Seq("xs" :: T(list)(tpeA), "f" :: (tpeA =>: tpeB))
    val retType: Type = T(list)(tpeB)
    val body: Seq[Variable] => Expr = { case Seq(xs, f) =>
      if_ (xs.isInstOf(T(cons)(tpeA))) {
        let ("cXs" :: T(cons)(tpeA), xs.asInstOf(T(cons)(tpeA))) { case cXs =>
          T(cons)(tpeB)(
            f(cXs.getField(head)),
            E(listMap)(tpeA, tpeB)(cXs.getField(tail), f))
        }
      } else_ {
        T(nil)(tpeB)()
      }
    }

    (args, retType, body)
  }

  val listFoldFunction = mkFunDef(listFold)("A") { case Seq(tpe) =>
    val args: Seq[ValDef] = Seq("xs" :: T(list)(tpe), "f" :: ((tpe, tpe) =>: tpe))
    val retType: Type = tpe
    val body: Seq[Variable] => Expr = { case Seq(xs, f) =>
      let ("cXs" :: T(cons)(tpe), xs.asInstOf(T(cons)(tpe))) { case cXs =>
        if_ (cXs.getField(tail).isInstOf(T(nil)(tpe))) {
          cXs.getField(head)
        } else_ {
          f(cXs.getField(head), E(listFold)(tpe)(cXs.getField(tail), f))
        }
      }
    }

    (args, retType, body)
  }

  val treeFoldFunction = mkFunDef(treeFold)("A") { case Seq(tpe) =>
    val args: Seq[ValDef] = Seq("t" :: T(tree)(tpe), "f" :: ((tpe, tpe) =>: tpe))
    val retType: Type = tpe
    val body: Seq[Variable] => Expr = { case Seq(t, f) =>
      if_ (t.isInstOf(T(branch)(tpe))) {
        let ("b" :: T(branch)(tpe), t.asInstOf(T(branch)(tpe))) { case b =>
          f(E(treeFold)(tpe)(b.getField(left), f), E(treeFold)(tpe)(b.getField(right), f))
        }
      } else_ {
        t.asInstOf(T(leaf)(tpe)).getField(value)
      }
    }

    (args, retType, body)
  }

  val program = InoxProgram(Context.empty, NoSymbols
    .withFunctions(Seq(concatenateFunction, toListFunction, listMapFunction, treeMapFunction, listFoldFunction, treeFoldFunction))
    .withADTs(Seq(listSort, consConstructor, nilConstructor, treeSort, branchConstructor, leafConstructor)))
  val theory = theoryOf(program)
  import theory._

  val tA = TypeParameter.fresh("A")
  val tB = TypeParameter.fresh("B")

  lazy val mapCommutesWithConcatenate = forallI("f" :: (tA =>: tB), "bs" :: T(list)(tA)) { case Seq(f, bs) =>
    def mapCommutes(as: Expr) =
      E(listMap)(tA, tB)(E(concatenate)(tA)(as, bs), f) ===
      E(concatenate)(tB)(E(listMap)(tA, tB)(as, f), E(listMap)(tA, tB)(bs, f))

    structuralInduction(mapCommutes _, T(list)(tA)) { case (ihs, goal) =>
      ihs.expression match {
        case C(`cons`, x, xs) => goal.by(ihs.hypothesis(xs))
        case C(`nil`) => goal.trivial
      }
    } 
  }

  lazy val mapCommutesWithToList = forallI("f" :: (tA =>: tB)) { case f => 

    def mapCommutes(t: Expr) =
      E(toList)(tB)(E(treeMap)(tA, tB)(t, f)) ===
      E(listMap)(tA, tB)(E(toList)(tA)(t), f) 

    structuralInduction(mapCommutes _, T(tree)(tA)) { case (ihs, goal) =>
      ihs.expression match {
        case C(`branch`, l, r) => {
          goal.by(andI(ihs.hypothesis(l), ihs.hypothesis(r), mapCommutesWithConcatenate))
        }
        case C(`leaf`, _) => goal.trivial
      }
    }
  }

  def isAssoc(f: Expr, tpe: Type) = forall("a" :: tpe, "b" :: tpe, "c" :: tpe) { case (a, b, c) => 
    f(a, f(b, c)) === f(f(a, b), c) 
  }

  lazy val splitListFold = forallI("f" :: ((tA, tA) =>: tA), "ys" :: T(list)(tA)) { case Seq(f, ys) => 
    implI(isAssoc(f, tA)) { (fIsAssoc: Theorem) =>
      implI(ys.isInstOf(T(cons)(tA))) { (ysNonEmpty: Theorem) =>
        
        def lhs(xs: Expr) = E(listFold)(tA)(E(concatenate)(tA)(xs, ys), f)
        def rhs(xs: Expr) = f(E(listFold)(tA)(xs, f), E(listFold)(tA)(ys, f))

        def property(xs: Expr) =
          xs.isInstOf(T(cons)(tA)) ==> {
            lhs(xs) === rhs(xs)
          }

        structuralInduction(property _, T(list)(tA)) { case (ihs, goal) =>
          ihs.expression match {
            case C(`cons`, h, t) => {

              val tIsNonEmptyCase: Theorem = implI(t.isInstOf(T(cons)(tA))) { (tNonEmpty: Theorem) =>

                lhs(ihs.expression)                                           ==| 
                                                                     ysNonEmpty |
                E(listFold)(tA)(T(cons)(tA)(h, E(concatenate)(tA)(t, ys)), f) ==| 
                                                                     ysNonEmpty |
                f(h, E(listFold)(tA)(E(concatenate)(tA)(t, ys), f))           ==|
                                 andI(ihs.hypothesis(t), tNonEmpty, ysNonEmpty) |
                f(h, f(E(listFold)(tA)(t, f), E(listFold)(tA)(ys, f)))        ==|
                                          andI(fIsAssoc, tNonEmpty, ysNonEmpty) |
                f(f(h, E(listFold)(tA)(t, f)), E(listFold)(tA)(ys, f))        ==| 
                                                    andI(tNonEmpty, ysNonEmpty) |
                rhs(ihs.expression)
              }

              val tIsEmptyCase: Theorem = implI(t.isInstOf(T(nil)(tA))) { (tEmpty: Theorem) =>

                lhs(ihs.expression)                                           ==| 
                                                                    (_.trivial) |
                E(listFold)(tA)(T(cons)(tA)(h, E(concatenate)(tA)(t, ys)), f) ==|
                                                                     ysNonEmpty |
                f(h, E(listFold)(tA)(E(concatenate)(tA)(t, ys), f))           ==| 
                                                       andI(tEmpty, ysNonEmpty) |
                f(h, E(listFold)(tA)(ys, f))                                  ==| 
                                                       andI(tEmpty, ysNonEmpty) |
                rhs(ihs.expression)
              }

              goal.by(andI(tIsNonEmptyCase, tIsEmptyCase))
            }
            case C(`nil`) => goal.trivial
          }
        }
      }
    }
  }

  lazy val toListNonEmpty = {
    def property(t: Expr) = E(toList)(tA)(t).isInstOf(T(cons)(tA))

    structuralInduction(property _, T(tree)(tA)) { case (ihs, goal) =>
      ihs.expression match {
        case C(`branch`, l, r) => goal.by(andI(ihs.hypothesis(l), ihs.hypothesis(r)))
        case C(`leaf`, _) => goal.trivial
      }
    }
  }

  lazy val foldTheorem = forallI("f" :: ((tA, tA) =>: tA)) { case f => 
    implI(isAssoc(f, tA)) { (fIsAssoc: Theorem) =>
      def property(t: Expr) =
        E(treeFold)(tA)(t, f) ===
        E(listFold)(tA)(E(toList)(tA)(t), f)

      structuralInduction(property _, T(tree)(tA)) { case (ihs, goal) =>
        ihs.expression match {
          case C(`branch`, l, r) => {

            val splitListFoldInstantiated = splitListFold
              .forallE(f, E(toList)(tA)(r))
              .implE(_.by(fIsAssoc))
              .implE(_.by(toListNonEmpty))
              .forallE(E(toList)(tA)(l))
              .implE(_.by(toListNonEmpty))

            val derivation =
              E(treeFold)(tA)(T(branch)(tA)(l, r), f)                                       ==|
                                                                                  (_.trivial) |
              f(E(treeFold)(tA)(l, f), E(treeFold)(tA)(r, f))                               ==|
                                                                            ihs.hypothesis(l) |
              f(E(listFold)(tA)(E(toList)(tA)(l), f), E(treeFold)(tA)(r, f))                ==|
                                                                            ihs.hypothesis(r) |
              f(E(listFold)(tA)(E(toList)(tA)(l), f), E(listFold)(tA)(E(toList)(tA)(r), f)) ==|
                                                                    splitListFoldInstantiated |
              E(listFold)(tA)(E(concatenate)(tA)(E(toList)(tA)(l), E(toList)(tA)(r)), f)    ==|
                                                                                  (_.trivial) |
              E(listFold)(tA)(E(toList)(tA)(T(branch)(tA)(l, r)), f)

            goal.by(derivation)
          }
          case C(`leaf`, _) => goal.trivial
        }
      }
    }
  }

  lazy val reformulatedFoldTheorem = forallI("f" :: ((tA, tA) =>: tA)) { case f => 
    implI(isAssoc(f, tA)) { (fIsAssoc: Theorem) =>
      forallI("t1" :: T(tree)(tA), "t2" :: T(tree)(tA)) { case Seq(t1, t2) =>
        implI(E(toList)(tA)(t1) === E(toList)(tA)(t2)) { (tsEqual: Theorem) =>

          val applied1 = forallE(implE(forallE(foldTheorem)(f))(_.by(fIsAssoc)))(t1)
          val applied2 = forallE(implE(forallE(foldTheorem)(f))(_.by(fIsAssoc)))(t2)

          E(treeFold)(tA)(t1, f)                ==|
                                         applied1 |
          E(listFold)(tA)(E(toList)(tA)(t1), f) ==|
                                          tsEqual |
          E(listFold)(tA)(E(toList)(tA)(t2), f) ==|
                                         applied2 |
          E(treeFold)(tA)(t2, f)
        }
      }
    }
  }
}
