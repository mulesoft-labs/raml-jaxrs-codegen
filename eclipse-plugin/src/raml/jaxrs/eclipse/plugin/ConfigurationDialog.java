/*
 * Copyright 2013 (c) MuleSoft, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package raml.jaxrs.eclipse.plugin;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.model.BaseWorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ConfigurationDialog extends TitleAreaDialog {	
	
	public ConfigurationDialog(Shell parentShell, UIConfiguration uiConfig) {
		super(parentShell);
		this.setShellStyle( SWT.DIALOG_TRIM|SWT.RESIZE );
		this.parentShell = parentShell;
		this.uiConfig = uiConfig;		
	}
	
	private final Shell parentShell;
	
	private final UIConfiguration uiConfig;
	
	protected Composite createDialogArea( Composite parent ){
		
		this.setTitle("Convert RAML to JAXRS");	
		parent.setLayout( new GridLayout(1,false) );
		Group hBox = new Group(parent,SWT.NONE);
		GridData hBoxData = new GridData( GridData.FILL_BOTH);
		hBoxData.widthHint = 300 ;
		hBoxData.heightHint = 300 ;
		hBox.setLayoutData( hBoxData ) ;
		hBox.setLayout( new GridLayout(1,false)) ;
		
		createTextField(hBox, "Base package name", this.uiConfig.basePackageName);
		
		createResourceSelectionGroup(hBox,"RAML file", this.uiConfig.ramlFile, "file");
		createResourceSelectionGroup(hBox,"Source folder", this.uiConfig.srcFolder, "folder");
		createResourceSelectionGroup(hBox,"Destination folder", this.uiConfig.dstFolder, "folder");
		
		return parent;	
	}

	private void createTextField(Group parent, String title, final ObjectReference<String> container)
	{
		Group group = new Group(parent, SWT.NONE);
		group.setText(title);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setLayout(new GridLayout(3,false));
		
		final Text text = new Text(group, SWT.BORDER);
		GridData textData = new GridData(GridData.FILL_HORIZONTAL);
		textData.horizontalSpan = 2;
		text.setLayoutData(textData);
		
		if( container.get() != null ){
			text.setText( container.get() );
		}
		text.addModifyListener( new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent e) {
				container.set(text.getText());				
			}
		});
	}

	private void createResourceSelectionGroup(
			Composite parent,
			String title,
			final ObjectReference<IResource> container,
			final String filterType ) {
		
		Group group = new Group(parent, SWT.NONE);
		group.setText(title);
		group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		group.setLayout(new GridLayout(3,false));
		
		final Text pathText = new Text(group, SWT.BORDER);
		GridData textData = new GridData(GridData.FILL_HORIZONTAL);
		textData.horizontalSpan = 2;
		pathText.setLayoutData(textData);
		
		if( container.get() != null ){
			pathText.setText( container.get().getFullPath().toString() );
		}
		
		pathText.setEditable(false);
		
		final Button pathButton = new Button(group, SWT.PUSH);
		GridData buttonData = new GridData();
		buttonData.horizontalSpan = 1;
		pathButton.setLayoutData(buttonData);
		pathButton.setText("Browse...");
		
		pathButton.addSelectionListener( new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				ElementTreeSelectionDialog dialog = new ElementTreeSelectionDialog(
						parentShell, new WorkbenchLabelProvider(), new BaseWorkbenchContentProvider());
				
				dialog.setAllowMultiple(false);				
				dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());				
				setFilter(filterType, dialog);
				
				if( dialog.open() != Dialog.OK ){
					return;
				}
				
				Object[] result = dialog.getResult();
				if(result==null||result.length==0){
					return;
				}
				if(!(result[0] instanceof IResource)){
					return;
				}
				IResource selectedResource = (IResource) result[0];
				container.set(selectedResource);
				pathText.setText( container.get().getFullPath().toString() );
			}			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {}
		});
	}


	
	private void setFilter(final String filterType,	ElementTreeSelectionDialog dialog)
	{
		if(filterType.equals("file")){
			dialog.addFilter(new ViewerFilter() {
				
				@Override
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					
					if(element==null){
						return false;
					}
					if(element instanceof IFile){
						IFile file = (IFile) element;
						return file.getName().toLowerCase().endsWith(".raml");
					}
					return true;
				}
			});
		}
		else if(filterType.equals("folder")){
			dialog.addFilter(new ViewerFilter() {
				
				@Override
				public boolean select(Viewer viewer, Object parentElement, Object element) {
					
					if(element==null){
						return false;
					}
					return element instanceof IContainer;
				}
			});
		}
	}	
}
