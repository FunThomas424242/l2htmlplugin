package l2html.editor.plugin.editors;

import java.io.StringWriter;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.StringTokenizer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;

/**
 * An example showing how to create a multi-page editor.
 * This example has 3 pages:
 * <ul>
 * <li>page 0 contains a nested text editor.
 * <li>page 1 shows the words in page 0 in sorted order
 * </ul>
 */
public class MultiPageEditor extends MultiPageEditorPart implements IResourceChangeListener {

	/** The text editor used in page 0. */
	private TextEditor editor;

	/** The text widget used in page 2. */
	private StyledText text;

	/**
	 * Creates a multi-page editor example.
	 */
	public MultiPageEditor() {
		super();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this);
	}

	/**
	 * Creates page 0 of the multi-page editor,
	 * which contains a text editor.
	 */
	void createPage0() {
		try {
			editor = new TextEditor();
			final int index = addPage(editor, getEditorInput());
			setPageText(index, editor.getTitle());
		} catch (final PartInitException e) {
			ErrorDialog.openError(getSite().getShell(), "Error creating nested text editor", null, e.getStatus());
		}
	}

	/**
	 * Creates page 1 of the multi-page editor,
	 * which shows the sorted text.
	 */
	void createPage1() {
		final Composite composite = new Composite(getContainer(), SWT.NONE);
		final FillLayout layout = new FillLayout();
		composite.setLayout(layout);
		text = new StyledText(composite, SWT.H_SCROLL | SWT.V_SCROLL);
		text.setEditable(false);

		final int index = addPage(composite);
		setPageText(index, "Preview");
	}

	/**
	 * Creates the pages of the multi-page editor.
	 */
	@Override
	protected void createPages() {
		createPage0();
		createPage1();
	}

	/**
	 * The <code>MultiPageEditorPart</code> implementation of this <code>IWorkbenchPart</code> method disposes all nested editors.
	 * Subclasses may extend.
	 */
	@Override
	public void dispose() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		super.dispose();
	}

	/**
	 * Saves the multi-page editor's document.
	 */
	@Override
	public void doSave(final IProgressMonitor monitor) {
		getEditor(0).doSave(monitor);
	}

	/**
	 * Saves the multi-page editor's document as another file.
	 * Also updates the text for page 0's tab, and updates this multi-page editor's input
	 * to correspond to the nested editor's.
	 */
	@Override
	public void doSaveAs() {
		final IEditorPart editor = getEditor(0);
		editor.doSaveAs();
		setPageText(0, editor.getTitle());
		setInput(editor.getEditorInput());
	}

	/* (non-Javadoc)
	 * Method declared on IEditorPart
	 */
	public void gotoMarker(final IMarker marker) {
		setActivePage(0);
		IDE.gotoMarker(getEditor(0), marker);
	}

	/**
	 * The <code>MultiPageEditorExample</code> implementation of this method
	 * checks that the input is an instance of <code>IFileEditorInput</code>.
	 */
	@Override
	public void init(final IEditorSite site, final IEditorInput editorInput) throws PartInitException {
		if (!(editorInput instanceof IFileEditorInput)) {
			throw new PartInitException("Invalid Input: Must be IFileEditorInput");
		}
		super.init(site, editorInput);
	}

	/* (non-Javadoc)
	 * Method declared on IEditorPart.
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}

	/**
	 * Calculates the contents of page 1 when the it is activated.
	 */
	@Override
	protected void pageChange(final int newPageIndex) {
		super.pageChange(newPageIndex);
		if (newPageIndex == 1) {
			sortWords();
		}
	}

	/**
	 * Closes all project files on project close.
	 */
	@Override
	public void resourceChanged(final IResourceChangeEvent event) {
		if (event.getType() == IResourceChangeEvent.PRE_CLOSE) {
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					final IWorkbenchPage[] pages = getSite().getWorkbenchWindow().getPages();
					for (int i = 0; i < pages.length; i++) {
						if (((FileEditorInput) editor.getEditorInput()).getFile().getProject().equals(event.getResource())) {
							final IEditorPart editorPart = pages[i].findEditor(editor.getEditorInput());
							pages[i].closeEditor(editorPart, true);
						}
					}
				}
			});
		}
	}

	/**
	 * Sorts the words in page 0, and shows them in page 1.
	 */
	void sortWords() {

		final String editorText = editor.getDocumentProvider().getDocument(editor.getEditorInput()).get();

		final StringTokenizer tokenizer = new StringTokenizer(editorText, " \t\n\r\f!@#\u0024%^&*()-_=+`~[]{};:'\",.<>/?|\\");
		final ArrayList<String> editorWords = new ArrayList<String>();
		while (tokenizer.hasMoreTokens()) {
			editorWords.add(tokenizer.nextToken());
		}

		Collections.sort(editorWords, Collator.getInstance());
		final StringWriter displayText = new StringWriter();
		for (int i = 0; i < editorWords.size(); i++) {
			displayText.write((editorWords.get(i)));
			displayText.write(System.getProperty("line.separator"));
		}
		text.setText(displayText.toString());
	}
}
