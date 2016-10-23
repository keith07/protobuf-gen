package edu.keith.protobuf.parser;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.DescriptorProtos.*;
import com.google.protobuf.Descriptors.*;
import com.google.protobuf.ExtensionRegistryLite;
import com.google.protobuf.WireFormat;
import com.google.protobuf.WireFormat.FieldType;
import com.squareup.javapoet.*;
import com.squareup.javapoet.TypeSpec.*;
import edu.keith.protobuf.model.PbFieldType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProtoLoader {
	private static final Logger logger = LoggerFactory.getLogger(ProtoLoader.class);

	private static final String sourceDir = "E:\\workspace\\lagou\\app-model-java\\src\\main\\resources\\";
	private static final String descDir = "E:\\generated\\desc\\";
//	private static final String[] sourceFiles = new String[]{sourceDir +  "*.proto",sourceDir +  "*/*.proto"};
	private static final String[] sourceFiles = new String[]{sourceDir +  "AdShowData.proto",sourceDir +  "ReturnId.proto",sourceDir +  "AdShow.proto"};
	private static final Map<String, FileDescriptorProto> fileDescriptorProtoMap = new HashMap<>();
	private static final Map<String, FileDescriptor> fileDescriptorMap = new HashMap<>();
	private static final Map<String, FileDescriptorProto> pdFileDescriptorProtoMap = new HashMap<>();

	private static final ClassName list = ClassName.get("java.util", "List");
	private static final ClassName arrayList = ClassName.get("java.util", "ArrayList");

	static {
		try {
			loadAll();
		} catch (Exception e) {
			logger.error("load proto error",e.getMessage());
		}
	}
	private static void loadAll() throws IOException, InterruptedException, DescriptorValidationException {

		//清空
		fileDescriptorProtoMap.clear();
		fileDescriptorMap.clear();
		pdFileDescriptorProtoMap.clear();

		Runtime run = Runtime.getRuntime();
		for (String sourceFile : sourceFiles) {
			File file = File.createTempFile("tmp", "desc");
			String cmd = "cmd /c " + sourceDir + "protoc.exe --proto_path==" + sourceDir + " --descriptor_set_out=" + file.getAbsolutePath() + " " + sourceFile;
			logger.debug("load all protoes:"+cmd);
			Process p = run.exec(cmd);
			// 如果不正常终止, 则生成desc文件失败
			if (p.waitFor() != 0) {
				if (p.exitValue() == 1) {//p.exitValue()==0表示正常结束，1：非正常结束
					logger.error("命令执行失败!",cmd);
					return ;
				}
			}
			FileInputStream fin = new FileInputStream(file);
			FileDescriptorSet descriptorSet = FileDescriptorSet.parseFrom(fin);

			for (FileDescriptorProto fdp: descriptorSet.getFileList()) {
				fileDescriptorProtoMap.put(fdp.getName(), fdp);
				for(DescriptorProto descriptor : fdp.getMessageTypeList())
					pdFileDescriptorProtoMap.put(descriptor.getName(), fdp);
			}
		}
		//填充fileDescriptorMap
		for (String key : fileDescriptorProtoMap.keySet()) {
			FileDescriptorProto fdp = fileDescriptorProtoMap.get(key);
			FileDescriptor fileDescriptor = parseToFileDescriptor(fdp);
			fileDescriptorMap.put(fdp.getName(), fileDescriptor);
			genJavaCode(fileDescriptor);
		}
	}

	/**
	 * 转换
	 *
	 * @param fdp
	 * @return
	 * @throws DescriptorValidationException
	 */
	private static FileDescriptor parseToFileDescriptor(FileDescriptorProto fdp) throws DescriptorValidationException {
		int dependencyCount = fdp.getDependencyCount();
		FileDescriptor[] fileDescriptors = new FileDescriptor[dependencyCount];
		for(int i = 0; i < dependencyCount; i++) {
			String dependcy = fdp.getDependency(i);
			fileDescriptors[i] = parseToFileDescriptor(fileDescriptorProtoMap.get(dependcy));
		}
		return FileDescriptor.buildFrom(fdp, fileDescriptors);
	}

	/**
	 * 生成java代码
	 *
	 * @param fileDescriptor
	 * @return
	 */
	private static void genJavaCode(FileDescriptor fileDescriptor) {
		StringBuilder stringBuilder = new StringBuilder();
		String entityName = fileDescriptor.getOptions().getJavaOuterClassname();
		String mainProtoName = fileDescriptor.getName();
		String packageName = fileDescriptor.getOptions().getJavaPackage();

		Builder mainEntityBuilder = TypeSpec.classBuilder(entityName)
				.addModifiers(Modifier.PUBLIC);

		List<Descriptor> descriptors = fileDescriptor.getMessageTypes();
		for(int i = 0; i < descriptors.size(); i++) {
			Descriptor descriptor = descriptors.get(i);
			if (i == 0) {//构建主类
				buildClass(mainEntityBuilder, descriptor);
			} else {
				Builder subClassBuilder = buildClass(TypeSpec.classBuilder(descriptor.getName()), descriptor);
				mainEntityBuilder.addType(subClassBuilder.build());
			}
		}

//		MethodSpec main = MethodSpec.methodBuilder("main")
//				.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
//				.returns(void.class)
//				.addParameter(String[].class, "args")
//				.addStatement("$T.out.println($S)", System.class, "Hello, JavaPoet!")
//				.build();

		TypeSpec mainEntity = mainEntityBuilder.build();
//				.addMethod(main)

		JavaFile javaFile = JavaFile.builder(packageName, mainEntity)
				.build();

		try {
			javaFile.writeTo(new File(descDir));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static Builder buildClass(Builder builder,Descriptor descriptor) {
		builder.addMethod(buildConstructorMethod());

		List<FieldDescriptor> fieldDescriptors = descriptor.getFields();
		for (FieldDescriptor fieldDescriptor : fieldDescriptors) {
			String fieldName = fieldDescriptor.getName();
			FieldDescriptor.JavaType javaType = fieldDescriptor.getJavaType();
			builder.addField(buildField(fieldDescriptor));
			builder.addMethod(buildGetMethod());
			builder.addMethod(buildSetMethod());
		}
		// TODO: 2016/10/23
		return builder;
	}

	private static MethodSpec buildConstructorMethod() {
		MethodSpec.Builder builder = MethodSpec.constructorBuilder()
				.addModifiers(Modifier.PUBLIC)
				.addParameter(CodedInputStream.class,"input")
				.addParameter(ExtensionRegistryLite.class,"extensionRegistry");

		// TODO: 2016/10/23
		return builder.build();
	}

	private static MethodSpec buildGetMethod() {
		// TODO: 2016/10/23
		MethodSpec.Builder flux = MethodSpec.methodBuilder("getFlux")
				.addModifiers(Modifier.PUBLIC);
		return flux.build();
	}

	private static MethodSpec buildSetMethod() {
		// TODO: 2016/10/23
		MethodSpec.Builder flux = MethodSpec.methodBuilder("flux")
				.addModifiers(Modifier.PUBLIC);
		return flux.build();
	}

	private static FieldSpec buildField(FieldDescriptor fieldDescriptor) {
		FieldType pbType = fieldDescriptor.getLiteType();
		TypeName typeName;
		if (pbType.equals(WireFormat.FieldType.MESSAGE)) {
			FileDescriptorProto descriptor = pdFileDescriptorProtoMap.get(fieldDescriptor.getMessageType().getName());
			typeName = ClassName.get(descriptor.getOptions().getJavaPackage(), descriptor.getOptions().getJavaOuterClassname());
		} else {
			PbFieldType fieldType = PbFieldType.getPbFieldType(pbType);
			typeName = fieldDescriptor.isOptional() ? fieldType.getType() : fieldType.getType().box();
		}
		typeName = fieldDescriptor.isRepeated() ? ParameterizedTypeName.get(list, typeName) : typeName;
		FieldSpec field = FieldSpec.builder(typeName, getFieldName(fieldDescriptor.getName(),fieldDescriptor.isRepeated()))
				.addModifiers(Modifier.PRIVATE)
				.build();
		return field;
	}

	private static String getFieldName(String name, boolean isRepeated) {
		int i = name.indexOf("_");
		String fieldName = i < 0 ? name : name.substring(i + 1);
		return isRepeated ? fieldName + "List" : fieldName;
	}

	public static void print() {
		System.out.println(1);
	}

	public static void main(String[] args) throws InterruptedException, DescriptorValidationException, IOException {
		ProtoLoader.print();
		System.out.println(1);
	}
}
