package ir.sharif.ce.parameteradderextractor.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.forge.roaster.Roaster;
import org.jboss.forge.roaster.model.JavaType;
import org.jboss.forge.roaster.model.JavaUnit;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaSource;
import org.jboss.forge.roaster.model.source.MethodSource;

public class JavaClassComparator {
	public static final String METHOD_SIGNATURE_SPEARATOR = ":::", METHOD_LIST_SEPARATOR = "$$$";

	private ArrayList<JavaClassSource> oldClasses, newClasses;
	private Set<String> oldMethodExtendedSignatures, newMethodExtendedSignatures;

	public JavaClassComparator(String oldFilePath, String newFilePath) throws IOException {
		setOldClasses(sourceFilePathToAllClasses(oldFilePath));
		setNewClasses(sourceFilePathToAllClasses(newFilePath));

		setOldMethodExtendedSignatures(classListToMethodSignatures(oldClasses));
		setNewMethodExtendedSignatures(classListToMethodSignatures(newClasses));
	}

	public Set<String> getMethodWithOneParameterAdded() {
		Set<String> ret = new HashSet<String>();
		for (String newMethod : newMethodExtendedSignatures) {
			if (oldMethodExtendedSignatures.contains(newMethod))
				continue;
			String newMethodWithoutLastParam = eliminateLastParam(newMethod);
			if (oldMethodExtendedSignatures.contains(newMethodWithoutLastParam))
				ret.add(newMethodWithoutLastParam + METHOD_LIST_SEPARATOR + newMethod);
		}
		return ret;
	}

	private String eliminateLastParam(String method) {
		if (method.contains(","))
			return method.substring(0, method.lastIndexOf(",")) + method.substring(method.lastIndexOf(")"));
		else
			return method.substring(0, method.lastIndexOf("(") + 1) + method.substring(method.lastIndexOf(")"));
	}

	public Set<String> classListToMethodSignatures(List<JavaClassSource> classes) {
		Set<String> ret = new HashSet<String>();
		for (JavaClassSource clazz : classes) {
			List<MethodSource<JavaClassSource>> methods = clazz.getMethods();
			for (MethodSource<JavaClassSource> method : methods) {
				ret.add(clazz.getName() + METHOD_SIGNATURE_SPEARATOR + method.toSignature());
			}
		}
		return ret;
	}

	public ArrayList<JavaClassSource> sourceFilePathToAllClasses(String filePath) throws IOException {
		File file = new File(filePath);
		InputStream fileStream = new FileInputStream(file);

		JavaUnit unit = Roaster.parseUnit(fileStream);
		List<JavaType<?>> types = unit.getTopLevelTypes();
		ArrayList<JavaClassSource> classes = new ArrayList<JavaClassSource>();
		for (int i = 0; i < types.size(); i++) {
			try {
				JavaClassSource clazz = (JavaClassSource) types.get(i);
				classes.addAll(extractAllClasses(clazz));
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}

		fileStream.close();

		return classes;
	}

	public List<JavaClassSource> extractAllClasses(JavaClassSource javaClassSource) {
		List<JavaClassSource> ret = new ArrayList<JavaClassSource>();
		ret.add(javaClassSource);
		List<JavaSource<?>> nestedTypes = javaClassSource.getNestedTypes();
		for (JavaSource<?> nestedType : nestedTypes) {
			try {
				JavaClassSource nestedClass = (JavaClassSource) nestedType;
				ret.add(nestedClass);
				ret.addAll(extractAllClasses(nestedClass));
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}
		}
		return ret;
	}

	public ArrayList<JavaClassSource> getOldClasses() {
		return oldClasses;
	}

	public void setOldClasses(ArrayList<JavaClassSource> oldClasses) {
		this.oldClasses = oldClasses;
	}

	public ArrayList<JavaClassSource> getNewClasses() {
		return newClasses;
	}

	public void setNewClasses(ArrayList<JavaClassSource> newClasses) {
		this.newClasses = newClasses;
	}

	public Set<String> getNewMethodExtendedSignatures() {
		return newMethodExtendedSignatures;
	}

	public void setNewMethodExtendedSignatures(Set<String> newMethodExtendedSignatures) {
		this.newMethodExtendedSignatures = newMethodExtendedSignatures;
	}

	public Set<String> getOldMethodExtendedSignatures() {
		return oldMethodExtendedSignatures;
	}

	public void setOldMethodExtendedSignatures(Set<String> oldMethodExtendedSignatures) {
		this.oldMethodExtendedSignatures = oldMethodExtendedSignatures;
	}
}
