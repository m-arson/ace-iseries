package com.ibm.swg.iseries.dataqueue;

import java.io.Serial;
import com.ibm.broker.config.appdev.InputTerminal;
import com.ibm.broker.config.appdev.Node;
import com.ibm.broker.config.appdev.NodeProperty;
import com.ibm.broker.config.appdev.OutputTerminal;

public class DataQueueGetNodeUDN extends Node {
	@Serial
	private static final long serialVersionUID = 1L;
	protected static final String NODE_TYPE_NAME = "com/ibm/swg/iseries/dataqueue/DataQueueGetNode";
	protected static final String NODE_GRAPHIC_16 = "platform:/plugin/ISeries/icons/full/obj16/com/ibm/swg/iseries/dataqueue/DataQueueGet.png";
	protected static final String NODE_GRAPHIC_32 = "platform:/plugin/ISeries/icons/full/obj30/com/ibm/swg/iseries/dataqueue/DataQueueGet.png";
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

	public static class ENUM_DATAQUEUEGET_SEARCHTYPE {
		private final String value;
		public static final ENUM_DATAQUEUEGET_SEARCHTYPE EQ = new ENUM_DATAQUEUEGET_SEARCHTYPE("EQ");
		public static final ENUM_DATAQUEUEGET_SEARCHTYPE NE = new ENUM_DATAQUEUEGET_SEARCHTYPE("NE");
		public static final ENUM_DATAQUEUEGET_SEARCHTYPE LT = new ENUM_DATAQUEUEGET_SEARCHTYPE("LT");
		public static final ENUM_DATAQUEUEGET_SEARCHTYPE LE = new ENUM_DATAQUEUEGET_SEARCHTYPE("LE");
		public static final ENUM_DATAQUEUEGET_SEARCHTYPE GT = new ENUM_DATAQUEUEGET_SEARCHTYPE("GT");
		public static final ENUM_DATAQUEUEGET_SEARCHTYPE GE = new ENUM_DATAQUEUEGET_SEARCHTYPE("GE");

		protected ENUM_DATAQUEUEGET_SEARCHTYPE(String value) {
			this.value = value;
		}

		public String toString() {
			return value;
		}

		protected static ENUM_DATAQUEUEGET_SEARCHTYPE getEnumFromString(String enumValue) {
			ENUM_DATAQUEUEGET_SEARCHTYPE enumConst = ENUM_DATAQUEUEGET_SEARCHTYPE.EQ;
			if (ENUM_DATAQUEUEGET_SEARCHTYPE.NE.value.equals(enumValue)) enumConst = ENUM_DATAQUEUEGET_SEARCHTYPE.NE;
			if (ENUM_DATAQUEUEGET_SEARCHTYPE.LT.value.equals(enumValue)) enumConst = ENUM_DATAQUEUEGET_SEARCHTYPE.LT;
			if (ENUM_DATAQUEUEGET_SEARCHTYPE.LE.value.equals(enumValue)) enumConst = ENUM_DATAQUEUEGET_SEARCHTYPE.LE;
			if (ENUM_DATAQUEUEGET_SEARCHTYPE.GT.value.equals(enumValue)) enumConst = ENUM_DATAQUEUEGET_SEARCHTYPE.GT;
			if (ENUM_DATAQUEUEGET_SEARCHTYPE.GE.value.equals(enumValue)) enumConst = ENUM_DATAQUEUEGET_SEARCHTYPE.GE;
			return enumConst;
		}

		public static String[] values = new String[]{ "EQ", "NE", "LT", "LE", "GT", "GE" };
	}

	protected NodeProperty[] getNodeProperties() {
		return new NodeProperty[] {
			new NodeProperty(DataQueueGetNodeUDN.PROPERTY_CONFIGURABLESERVICE,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, "myISeries","","",	"com/ibm/swg/iseries/dataqueue/DataQueueGet",	"ISeries"),
			new NodeProperty(DataQueueGetNodeUDN.PROPERTY_QUEUENAME,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, null,"","",	"com/ibm/swg/iseries/dataqueue/DataQueueGet",	"ISeries"),
			new NodeProperty(DataQueueGetNodeUDN.PROPERTY_WAITINTERVAL,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.INTEGER, "0","","",	"com/ibm/swg/iseries/dataqueue/DataQueueGet",	"ISeries"),
			new NodeProperty(DataQueueGetNodeUDN.PROPERTY_LIBRARY,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, null,"","",	"com/ibm/swg/iseries/dataqueue/DataQueueGet",	"ISeries"),
			new NodeProperty(DataQueueGetNodeUDN.PROPERTY_TYPE,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, "DTAQ","","",	"com/ibm/swg/iseries/dataqueue/DataQueueGet",	"ISeries"),
			new NodeProperty(DataQueueGetNodeUDN.PROPERTY_KEYED,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.BOOLEAN, "false","","",	"com/ibm/swg/iseries/dataqueue/DataQueueGet",	"ISeries"),
			new NodeProperty(DataQueueGetNodeUDN.PROPERTY_KEY,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, null,"","",	"com/ibm/swg/iseries/dataqueue/DataQueueGet",	"ISeries"),
			new NodeProperty(DataQueueGetNodeUDN.PROPERTY_KEYLENGTH,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.INTEGER, null,"","",	"com/ibm/swg/iseries/dataqueue/DataQueueGet",	"ISeries"),
			new NodeProperty(DataQueueGetNodeUDN.PROPERTY_SEARCHTYPE,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.ENUMERATION, "EQ", ENUM_DATAQUEUEGET_SEARCHTYPE.class,"","",	"com/ibm/swg/iseries/dataqueue/DataQueueGet",	"ISeries"),
			new NodeProperty(DataQueueGetNodeUDN.PROPERTY_ENABLE,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.BOOLEAN, "false","","",	"com/ibm/swg/iseries/dataqueue/DataQueueGet",	"ISeries"),
			new NodeProperty(DataQueueGetNodeUDN.PROPERTY_DIRECTORY,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, null,"","",	"com/ibm/swg/iseries/dataqueue/DataQueueGet",	"ISeries")
		};
	}

	public DataQueueGetNodeUDN() {
	}

	public final InputTerminal INPUT_TERMINAL_IN = new InputTerminal(this, "InTerminal.in");

	@Override
	public InputTerminal[] getInputTerminals() {
		return new InputTerminal[] {
			INPUT_TERMINAL_IN
		};
	}

	public final OutputTerminal OUTPUT_TERMINAL_NOMESSAGE = new OutputTerminal(this, "OutTerminal.noMessage");
	public final OutputTerminal OUTPUT_TERMINAL_FAILURE = new OutputTerminal(this, "OutTerminal.failure");
	public final OutputTerminal OUTPUT_TERMINAL_OUT = new OutputTerminal(this, "OutTerminal.out");

	@Override
	public OutputTerminal[] getOutputTerminals() {
		return new OutputTerminal[] {
			OUTPUT_TERMINAL_NOMESSAGE,
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

	public DataQueueGetNodeUDN setConfigurableService(String value) {
		setProperty(DataQueueGetNodeUDN.PROPERTY_CONFIGURABLESERVICE, value);
		return this;
	}

	public String getConfigurableService() {
		return (String)getPropertyValue(DataQueueGetNodeUDN.PROPERTY_CONFIGURABLESERVICE);
	}

	public DataQueueGetNodeUDN setQueueName(String value) {
		setProperty(DataQueueGetNodeUDN.PROPERTY_QUEUENAME, value);
		return this;
	}

	public String getQueueName() {
		return (String)getPropertyValue(DataQueueGetNodeUDN.PROPERTY_QUEUENAME);
	}

	public DataQueueGetNodeUDN setWaitInterval(int value) {
		setProperty(DataQueueGetNodeUDN.PROPERTY_WAITINTERVAL, Integer.toString(value));
		return this;
	}

	public int getWaitInterval() {
		String value = (String)getPropertyValue(DataQueueGetNodeUDN.PROPERTY_WAITINTERVAL);
		return Integer.parseInt(value);
	}

	public DataQueueGetNodeUDN setLibrary(String value) {
		setProperty(DataQueueGetNodeUDN.PROPERTY_LIBRARY, value);
		return this;
	}

	public String getLibrary() {
		return (String)getPropertyValue(DataQueueGetNodeUDN.PROPERTY_LIBRARY);
	}

	public DataQueueGetNodeUDN setType(String value) {
		setProperty(DataQueueGetNodeUDN.PROPERTY_TYPE, value);
		return this;
	}

	public String getType() {
		return (String)getPropertyValue(DataQueueGetNodeUDN.PROPERTY_TYPE);
	}

	public DataQueueGetNodeUDN setKeyed(boolean value) {
		setProperty(DataQueueGetNodeUDN.PROPERTY_KEYED, String.valueOf(value));
		return this;
	}

	public boolean getKeyed() {
		return "true".equals(getPropertyValue(DataQueueGetNodeUDN.PROPERTY_KEYED));
	}

	public DataQueueGetNodeUDN setKey(String value) {
		setProperty(DataQueueGetNodeUDN.PROPERTY_KEY, value);
		return this;
	}

	public String getKey() {
		return (String)getPropertyValue(DataQueueGetNodeUDN.PROPERTY_KEY);
	}

	public DataQueueGetNodeUDN setKeylength(int value) {
		setProperty(DataQueueGetNodeUDN.PROPERTY_KEYLENGTH, Integer.toString(value));
		return this;
	}

	public int getKeylength() {
		String value = (String)getPropertyValue(DataQueueGetNodeUDN.PROPERTY_KEYLENGTH);
		return Integer.parseInt(value);
	}

	public DataQueueGetNodeUDN setSearchType(ENUM_DATAQUEUEGET_SEARCHTYPE value) {
		setProperty(DataQueueGetNodeUDN.PROPERTY_SEARCHTYPE, value.toString());
		return this;
	}

	public ENUM_DATAQUEUEGET_SEARCHTYPE getSearchType() {
		return ENUM_DATAQUEUEGET_SEARCHTYPE.getEnumFromString((String)getPropertyValue(DataQueueGetNodeUDN.PROPERTY_SEARCHTYPE));
	}

	public DataQueueGetNodeUDN setEnable(boolean value) {
		setProperty(DataQueueGetNodeUDN.PROPERTY_ENABLE, String.valueOf(value));
		return this;
	}

	public boolean getEnable() {
		return "true".equals(getPropertyValue(DataQueueGetNodeUDN.PROPERTY_ENABLE));
	}

	public DataQueueGetNodeUDN setDirectory(String value) {
		setProperty(DataQueueGetNodeUDN.PROPERTY_DIRECTORY, value);
		return this;
	}

	public String getDirectory() {
		return (String)getPropertyValue(DataQueueGetNodeUDN.PROPERTY_DIRECTORY);
	}

	public String getNodeName() {
		String retVal = super.getNodeName();
		if (retVal == null || retVal.isEmpty()) {
			retVal = "DataQueueGet";
		}
		return retVal;
	}
}
