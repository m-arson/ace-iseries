package com.ibm.swg.iseries.dataqueue;

import java.io.Serial;
import com.ibm.broker.config.appdev.InputTerminal;
import com.ibm.broker.config.appdev.Node;
import com.ibm.broker.config.appdev.NodeProperty;
import com.ibm.broker.config.appdev.OutputTerminal;

public class DataQueueInputNodeUDN extends Node {
	@Serial
	private static final long serialVersionUID = 1L;
	protected static final String NODE_TYPE_NAME = "com/ibm/swg/iseries/dataqueue/DataQueueInputNode";
	protected static final String NODE_GRAPHIC_16 = "platform:/plugin/ISeries/icons/full/obj16/com/ibm/swg/iseries/dataqueue/DataQueueInput.png";
	protected static final String NODE_GRAPHIC_32 = "platform:/plugin/ISeries/icons/full/obj30/com/ibm/swg/iseries/dataqueue/DataQueueInput.png";
	protected static final String PROPERTY_CONFIGURABLESERVICE = "configurableService";
	protected static final String PROPERTY_QUEUENAME = "queueName";
	protected static final String PROPERTY_WAITINTERVAL = "waitInterval";
	protected static final String PROPERTY_LIBRARY = "library";
	protected static final String PROPERTY_TYPE = "type";
	protected static final String PROPERTY_KEYED = "keyed";
	protected static final String PROPERTY_KEY = "key";
	protected static final String PROPERTY_KEYLENGTH = "keylength";
	protected static final String PROPERTY_SEARCHTYPE = "searchType";
	protected static final String PROPERTY_ENABLE = "enable";
	protected static final String PROPERTY_DIRECTORY = "directory";

	public static class ENUM_DATAQUEUEINPUT_SEARCHTYPE {
		private final String value;
		public static final ENUM_DATAQUEUEINPUT_SEARCHTYPE EQ = new ENUM_DATAQUEUEINPUT_SEARCHTYPE("EQ");
		public static final ENUM_DATAQUEUEINPUT_SEARCHTYPE NE = new ENUM_DATAQUEUEINPUT_SEARCHTYPE("NE");
		public static final ENUM_DATAQUEUEINPUT_SEARCHTYPE LT = new ENUM_DATAQUEUEINPUT_SEARCHTYPE("LT");
		public static final ENUM_DATAQUEUEINPUT_SEARCHTYPE LE = new ENUM_DATAQUEUEINPUT_SEARCHTYPE("LE");
		public static final ENUM_DATAQUEUEINPUT_SEARCHTYPE GT = new ENUM_DATAQUEUEINPUT_SEARCHTYPE("GT");
		public static final ENUM_DATAQUEUEINPUT_SEARCHTYPE GE = new ENUM_DATAQUEUEINPUT_SEARCHTYPE("GE");

		protected ENUM_DATAQUEUEINPUT_SEARCHTYPE(String value) {
			this.value = value;
		}

		public String toString() {
			return value;
		}

		protected static ENUM_DATAQUEUEINPUT_SEARCHTYPE getEnumFromString(String enumValue) {
			ENUM_DATAQUEUEINPUT_SEARCHTYPE enumConst = ENUM_DATAQUEUEINPUT_SEARCHTYPE.EQ;
			if (ENUM_DATAQUEUEINPUT_SEARCHTYPE.NE.value.equals(enumValue)) enumConst = ENUM_DATAQUEUEINPUT_SEARCHTYPE.NE;
			if (ENUM_DATAQUEUEINPUT_SEARCHTYPE.LT.value.equals(enumValue)) enumConst = ENUM_DATAQUEUEINPUT_SEARCHTYPE.LT;
			if (ENUM_DATAQUEUEINPUT_SEARCHTYPE.LE.value.equals(enumValue)) enumConst = ENUM_DATAQUEUEINPUT_SEARCHTYPE.LE;
			if (ENUM_DATAQUEUEINPUT_SEARCHTYPE.GT.value.equals(enumValue)) enumConst = ENUM_DATAQUEUEINPUT_SEARCHTYPE.GT;
			if (ENUM_DATAQUEUEINPUT_SEARCHTYPE.GE.value.equals(enumValue)) enumConst = ENUM_DATAQUEUEINPUT_SEARCHTYPE.GE;
			return enumConst;
		}

		public static String[] values = new String[]{ "EQ", "NE", "LT", "LE", "GT", "GE" };
	}

	protected NodeProperty[] getNodeProperties() {
		return new NodeProperty[] {
			new NodeProperty(DataQueueInputNodeUDN.PROPERTY_CONFIGURABLESERVICE,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, "myISeries","","",	"com/ibm/swg/iseries/dataqueue/DataQueueInput",	"ISeries"),
			new NodeProperty(DataQueueInputNodeUDN.PROPERTY_QUEUENAME,		NodeProperty.Usage.MANDATORY,	false,	NodeProperty.Type.STRING, null,"","",	"com/ibm/swg/iseries/dataqueue/DataQueueInput",	"ISeries"),
			new NodeProperty(DataQueueInputNodeUDN.PROPERTY_WAITINTERVAL,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.INTEGER, "0","","",	"com/ibm/swg/iseries/dataqueue/DataQueueInput",	"ISeries"),
			new NodeProperty(DataQueueInputNodeUDN.PROPERTY_LIBRARY,		NodeProperty.Usage.MANDATORY,	false,	NodeProperty.Type.STRING, null,"","",	"com/ibm/swg/iseries/dataqueue/DataQueueInput",	"ISeries"),
			new NodeProperty(DataQueueInputNodeUDN.PROPERTY_TYPE,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, "DTAQ","","",	"com/ibm/swg/iseries/dataqueue/DataQueueInput",	"ISeries"),
			new NodeProperty(DataQueueInputNodeUDN.PROPERTY_KEYED,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.BOOLEAN, "false","","",	"com/ibm/swg/iseries/dataqueue/DataQueueInput",	"ISeries"),
			new NodeProperty(DataQueueInputNodeUDN.PROPERTY_KEY,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, null,"","",	"com/ibm/swg/iseries/dataqueue/DataQueueInput",	"ISeries"),
			new NodeProperty(DataQueueInputNodeUDN.PROPERTY_KEYLENGTH,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.INTEGER, null,"","",	"com/ibm/swg/iseries/dataqueue/DataQueueInput",	"ISeries"),
			new NodeProperty(DataQueueInputNodeUDN.PROPERTY_SEARCHTYPE,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.ENUMERATION, "EQ", ENUM_DATAQUEUEINPUT_SEARCHTYPE.class,"","",	"com/ibm/swg/iseries/dataqueue/DataQueueInput",	"ISeries"),
			new NodeProperty(DataQueueInputNodeUDN.PROPERTY_ENABLE,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.BOOLEAN, "false","","",	"com/ibm/swg/iseries/dataqueue/DataQueueInput",	"ISeries"),
			new NodeProperty(DataQueueInputNodeUDN.PROPERTY_DIRECTORY,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, null,"","",	"com/ibm/swg/iseries/dataqueue/DataQueueInput",	"ISeries")
		};
	}

	public DataQueueInputNodeUDN() {
	}

	@Override
	public InputTerminal[] getInputTerminals() {
		return null;
	}

	@Override
	public InputTerminal getInputTerminal(String terminalName) {
		return null;
	}

	public final OutputTerminal OUTPUT_TERMINAL_FAILURE = new OutputTerminal(this, "OutTerminal.failure");
	public final OutputTerminal OUTPUT_TERMINAL_OUT = new OutputTerminal(this, "OutTerminal.out");

	@Override
	public OutputTerminal[] getOutputTerminals() {
		return new OutputTerminal[] {
			OUTPUT_TERMINAL_FAILURE,
			OUTPUT_TERMINAL_OUT
		};
	}

	@Override
	public String getTypeName() {
		return NODE_TYPE_NAME;
	}

	protected String getGraphic16() {
		return NODE_GRAPHIC_16;
	}

	protected String getGraphic32() {
		return NODE_GRAPHIC_32;
	}

	public DataQueueInputNodeUDN setConfigurableService(String value) {
		setProperty(DataQueueInputNodeUDN.PROPERTY_CONFIGURABLESERVICE, value);
		return this;
	}

	public String getConfigurableService() {
		return (String)getPropertyValue(DataQueueInputNodeUDN.PROPERTY_CONFIGURABLESERVICE);
	}

	public DataQueueInputNodeUDN setQueueName(String value) {
		setProperty(DataQueueInputNodeUDN.PROPERTY_QUEUENAME, value);
		return this;
	}

	public String getQueueName() {
		return (String)getPropertyValue(DataQueueInputNodeUDN.PROPERTY_QUEUENAME);
	}

	public DataQueueInputNodeUDN setWaitInterval(int value) {
		setProperty(DataQueueInputNodeUDN.PROPERTY_WAITINTERVAL, Integer.toString(value));
		return this;
	}

	public int getWaitInterval() {
		String value = (String)getPropertyValue(DataQueueInputNodeUDN.PROPERTY_WAITINTERVAL);
		return Integer.parseInt(value);
	}

	public DataQueueInputNodeUDN setLibrary(String value) {
		setProperty(DataQueueInputNodeUDN.PROPERTY_LIBRARY, value);
		return this;
	}

	public String getLibrary() {
		return (String)getPropertyValue(DataQueueInputNodeUDN.PROPERTY_LIBRARY);
	}

	public DataQueueInputNodeUDN setType(String value) {
		setProperty(DataQueueInputNodeUDN.PROPERTY_TYPE, value);
		return this;
	}

	public String getType() {
		return (String)getPropertyValue(DataQueueInputNodeUDN.PROPERTY_TYPE);
	}

	public DataQueueInputNodeUDN setKeyed(boolean value) {
		setProperty(DataQueueInputNodeUDN.PROPERTY_KEYED, String.valueOf(value));
		return this;
	}

	public boolean getKeyed() {
		return "true".equals(getPropertyValue(DataQueueInputNodeUDN.PROPERTY_KEYED));
	}

	public DataQueueInputNodeUDN setKey(String value) {
		setProperty(DataQueueInputNodeUDN.PROPERTY_KEY, value);
		return this;
	}

	public String getKey() {
		return (String)getPropertyValue(DataQueueInputNodeUDN.PROPERTY_KEY);
	}

	public DataQueueInputNodeUDN setKeylength(int value) {
		setProperty(DataQueueInputNodeUDN.PROPERTY_KEYLENGTH, Integer.toString(value));
		return this;
	}

	public int getKeylength() {
		String value = (String)getPropertyValue(DataQueueInputNodeUDN.PROPERTY_KEYLENGTH);
		return Integer.parseInt(value);
	}

	public DataQueueInputNodeUDN setSearchType(ENUM_DATAQUEUEINPUT_SEARCHTYPE value) {
		setProperty(DataQueueInputNodeUDN.PROPERTY_SEARCHTYPE, value.toString());
		return this;
	}

	public ENUM_DATAQUEUEINPUT_SEARCHTYPE getSearchType() {
		return ENUM_DATAQUEUEINPUT_SEARCHTYPE.getEnumFromString((String)getPropertyValue(DataQueueInputNodeUDN.PROPERTY_SEARCHTYPE));
	}

	public DataQueueInputNodeUDN setEnable(boolean value) {
		setProperty(DataQueueInputNodeUDN.PROPERTY_ENABLE, String.valueOf(value));
		return this;
	}

	public boolean getEnable() {
		return "true".equals(getPropertyValue(DataQueueInputNodeUDN.PROPERTY_ENABLE));
	}

	public DataQueueInputNodeUDN setDirectory(String value) {
		setProperty(DataQueueInputNodeUDN.PROPERTY_DIRECTORY, value);
		return this;
	}

	public String getDirectory() {
		return (String)getPropertyValue(DataQueueInputNodeUDN.PROPERTY_DIRECTORY);
	}

	public String getNodeName() {
		String retVal = super.getNodeName();
		if (retVal == null || retVal.isEmpty()) {
			retVal = "DataQueueInput";
		}
		return retVal;
	}
}
