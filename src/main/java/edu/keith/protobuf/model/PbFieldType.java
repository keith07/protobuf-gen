package edu.keith.protobuf.model;

import com.google.protobuf.WireFormat.FieldType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.TypeName;

/**
 * Created by keith on 2016/10/23.
 */
public enum PbFieldType {

	DOUBLE(TypeName.DOUBLE, "0"),
	FLOAT(TypeName.FLOAT, "0f"),
	INT64(TypeName.LONG, "0L"),
	INT32(TypeName.INT, "0"),
	BOOL(TypeName.BOOLEAN, "false"),
	STRING(TypeName.get(String.class), ""),
	OBJECT(TypeName.OBJECT, "null");

	PbFieldType(TypeName type, String defaultValue) {
		this.type = type;
		this.defaultValue = defaultValue;
	}

	public static PbFieldType getPbFieldType(FieldType fieldType) {
		if (FieldType.DOUBLE.equals(fieldType)) {
			return DOUBLE;
		} else if (FieldType.FLOAT.equals(fieldType)) {
			return FLOAT;
		} else if (FieldType.INT64.equals(fieldType)) {
			return INT64;
		} else if (FieldType.INT32.equals(fieldType)) {
			return INT32;
		} else if (FieldType.BOOL.equals(fieldType)) {
			return BOOL;
		} else if (FieldType.STRING.equals(fieldType)) {
			return STRING;
		}
		return OBJECT;// TODO: 2016/10/23  
	}

	private TypeName type;
	private String defaultValue;

	public TypeName getType() {
		return type;
	}

	public String getDefaultValue() {
		return defaultValue;
	}
}
