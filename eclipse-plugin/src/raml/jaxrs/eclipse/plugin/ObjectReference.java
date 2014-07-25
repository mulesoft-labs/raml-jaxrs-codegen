package raml.jaxrs.eclipse.plugin;

public class ObjectReference<T> {
	
	T object ;

	public T get() {
		return object;
	}

	public void set(T object) {
		this.object = object;
	}

}
