
type Modifier = {}
type V8ValueFunction = (...any) => any
type ImageVector = {}


interface ComposeElement {
    tag: string
    id: number
    modifier: Modifier
    parentNode: ComposeElement | null
    props: Record<string, any>
    children: ComposeElement[]
    removeChild(child: ComposeElement)
    insertChild(child: ComposeElement, ref: ComposeElement | null)
    addTemplate(name: String, element: ComposeElement)
    removeTemplate(name: String)
}
interface ComposeTextNode extends ComposeElement {
    text: string
}
interface ModifierBuilder {
    modifier: Modifier
    width(width: Int)
    height(height: Int)
    rotate(rotate: Float)
    padding(padding: Int)
    padding(horizontal: Int, vertical: Int)
    padding(left: Int, top: Int, right: Int, bottom: Int)
    click(callback: V8ValueFunction)
    fillMaxSize()
    fillMaxWidth()
    fillMaxHeight()
    background(color: any)
}
declare namespace root.ui {
    function createModifierBuilder(modifier: Modifier | null): ModifierBuilder
    function createComposeElement(
        tag: string,
        props: Record<string, any>,
        ...children: any[]): ComposeElement
    function createComposeText(text: string): ComposeElement
    function startActivity(
        element: ComposeElement,
        listener: null | ((event: string, ...args: any[]) => any)): Promise<Activity>
    function patchProp(element: ComposeElement, key: String, value: any)
    function updateComposeElement(element: ComposeElement)
    function loadIcon(
        group: string,
        name: String): ImageVector
}