annotation class Anno(val position: String)

class MyClass {
    <!WRONG_ANNOTATION_TARGET!>@Anno("init $<!UNRESOLVED_REFERENCE!>prop<!>")<!> init {

    }

    companion object {
        private const val prop = 0
    }
}
