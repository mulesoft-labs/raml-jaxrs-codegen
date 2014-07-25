package raml.jaxrs.eclipse.plugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.Workbench;
import org.raml.jaxrs.codegen.core.Configuration;
import org.raml.jaxrs.codegen.core.Generator;

public class GenerationHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		ISelection selection = Workbench.getInstance().getActiveWorkbenchWindow().getActivePage().getSelection();
		if(selection instanceof IStructuredSelection){
			IStructuredSelection sSelection = (IStructuredSelection) selection;
			Object element = sSelection.getFirstElement();
			if(element instanceof IFile){
				IFile file = (IFile) element;
				process(file);
			}
			return null;
			
		}
		return null;
	}

	private void process(IFile file) {
		
		Shell activeShell = Display.getCurrent().getActiveShell();
		UIConfiguration uiConfig = prepareUIConfiguration(file);
		ConfigurationDialog dialog = new ConfigurationDialog(activeShell, uiConfig);
		
		if(dialog.open() != Dialog.OK ){
			return ;
		}
		
		if(!uiConfig.isValid()){
			return;
		}
		
		Configuration configuration = prepareConfiguraton(uiConfig);
		
		try {
			File ramlOSFile = uiConfig.getRamlFile().getLocation().toFile();
			InputStreamReader ramlReader = new InputStreamReader( new FileInputStream(ramlOSFile) );
			new Generator().run(ramlReader, configuration);		
			uiConfig.getDstFolder().refreshLocal( IResource.DEPTH_ONE, new NullProgressMonitor() );
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private Configuration prepareConfiguraton(UIConfiguration uiConfig)
	{
		IContainer srcFolder = uiConfig.getSrcFolder();
		if(srcFolder == null){
			srcFolder = uiConfig.getRamlFile().getParent();
		}
		File srcOSFolder = srcFolder.getLocation().toFile();		
		File dstOSFolder = uiConfig.getDstFolder().getLocation().toFile();
		
		Configuration configuration = new Configuration();
		configuration.setOutputDirectory(dstOSFolder);
		configuration.setSourceDirectory(srcOSFolder);
		configuration.setBasePackageName(uiConfig.getBasePackageName());
		return configuration;
	}

	private UIConfiguration prepareUIConfiguration(IFile file) {
		
		UIConfiguration uiConfig = new UIConfiguration();
		uiConfig.setRamlFile(file);
		uiConfig.setSrcFolder(file.getParent());
		uiConfig.setDstFolder(null);
		uiConfig.setBasePackageName("org.raml.jaxrs.test");
		return uiConfig;
	}
}
