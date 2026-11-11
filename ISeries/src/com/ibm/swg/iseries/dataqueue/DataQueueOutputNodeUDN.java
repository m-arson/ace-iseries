package com.ibm.swg.iseries.dataqueue;

import java.io.Serial;
import com.ibm.broker.config.appdev.InputTerminal;
import com.ibm.broker.config.appdev.Node;
import com.ibm.broker.config.appdev.NodeProperty;
import com.ibm.broker.config.appdev.OutputTerminal;

public class DataQueueOutputNodeUDN extends Node {
	@Serial
	private static final long serialVersionUID = 1L;
	protected static final String NODE_TYPE_NAME = "com/ibm/swg/iseries/dataqueue/DataQueueOutputNode";
	protected static final String NODE_GRAPHIC_16 = "platform:/plugin/ISeries/icons/full/obj16/com/ibm/swg/iseries/dataqueue/DataQueueOutput.png";
	protected static final String NODE_GRAPHIC_32 = "platform:/plugin/ISeries/icons/full/obj30/com/ibm/swg/iseries/dataqueue/DataQueueOutput.png";
	protected static final String PROPERTY_CONFIGURABLESERVICE = "configurableService";
	protected static final String PROPERTY_QUEUENAME = "queueName";
	protected static final String PROPERTY_LIBRARY = "library";
	protected static final String PROPERTY_TYPE = "type";
	protected static final String PROPERTY_KEYED = "keyed";
	protected static final String PROPERTY_KEY = "key";
	protected static final String PROPERTY_KEYLENGTH = "keylength";
	protected static final String PROPERTY_ENABLE = "enable";
	protected static final String PROPERTY_DIRECTORY = "directory";

	protected NodeProperty[] getNodeProperties() {
		return new NodeProperty[] {
			new NodeProperty(DataQueueOutputNodeUDN.PROPERTY_CONFIGURABLESERVICE,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, "myISeries","","",	"com/ibm/swg/iseries/dataqueue/DataQueueOutput",	"ISeries"),
			new NodeProperty(DataQueueOutputNodeUDN.PROPERTY_QUEUENAME,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, null,"","",	"com/ibm/swg/iseries/dataqueue/DataQueueOutput",	"ISeries"),
			new NodeProperty(DataQueueOutputNodeUDN.PROPERTY_LIBRARY,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, null,"","",	"com/ibm/swg/iseries/dataqueue/DataQueueOutput",	"ISeries"),
			new NodeProperty(DataQueueOutputNodeUDN.PROPERTY_TYPE,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, "DTAQ","","",	"com/ibm/swg/iseries/dataqueue/DataQueueOutput",	"ISeries"),
			new NodeProperty(DataQueueOutputNodeUDN.PROPERTY_KEYED,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.BOOLEAN, "false","","",	"com/ibm/swg/iseries/dataqueue/DataQueueOutput",	"ISeries"),
			new NodeProperty(DataQueueOutputNodeUDN.PROPERTY_KEY,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, null,"","",	"com/ibm/swg/iseries/dataqueue/DataQueueOutput",	"ISeries"),
			new NodeProperty(DataQueueOutputNodeUDN.PROPERTY_KEYLENGTH,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.INTEGER, null,"","",	"com/ibm/swg/iseries/dataqueue/DataQueueOutput",	"ISeries"),
			new NodeProperty(DataQueueOutputNodeUDN.PROPERTY_ENABLE,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.BOOLEAN, "false","","",	"com/ibm/swg/iseries/dataqueue/DataQueueOutput",	"ISeries"),
			new NodeProperty(DataQueueOutputNodeUDN.PROPERTY_DIRECTORY,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, "","","",	"com/ibm/swg/iseries/dataqueue/DataQueueOutput",	"ISeries")
		};
	}

	public DataQueueOutputNodeUDN() {
	}

	public final InputTerminal INPUT_TERMINAL_IN = new InputTerminal(this, "InTerminal.in");

	@Override
	public InputTerminal[] getInputTerminals() {
		return new InputTerminal[] {
			INPUT_TERMINAL_IN
		};
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

	public DataQueueOutputNodeUDN setConfigurableService(String value) {
		setProperty(DataQueueOutputNodeUDN.PROPERTY_CONFIGURABLESERVICE, value);
		return this;
	}

	public String getConfigurableService() {
		return (String)getPropertyValue(DataQueueOutputNodeUDN.PROPERTY_CONFIGURABLESERVICE);
	}

	public DataQueueOutputNodeUDN setQueueName(String value) {
		setProperty(DataQueueOutputNodeUDN.PROPERTY_QUEUENAME, value);
		return this;
	}

	public String getQueueName() {
		return (String)getPropertyValue(DataQueueOutputNodeUDN.PROPERTY_QUEUENAME);
	}

	public DataQueueOutputNodeUDN setLibrary(String value) {
		setProperty(DataQueueOutputNodeUDN.PROPERTY_LIBRARY, value);
		return this;
	}

	public String getLibrary() {
		return (String)getPropertyValue(DataQueueOutputNodeUDN.PROPERTY_LIBRARY);
	}

	public DataQueueOutputNodeUDN setType(String value) {
		setProperty(DataQueueOutputNodeUDN.PROPERTY_TYPE, value);
		return this;
	}

	public String getType() {
		return (String)getPropertyValue(DataQueueOutputNodeUDN.PROPERTY_TYPE);
	}

	public DataQueueOutputNodeUDN setKeyed(boolean value) {
		setProperty(DataQueueOutputNodeUDN.PROPERTY_KEYED, String.valueOf(value));
		return this;
	}

	public boolean getKeyed() {
		return "true".equals(getPropertyValue(DataQueueOutputNodeUDN.PROPERTY_KEYED));
	}

	public DataQueueOutputNodeUDN setKey(String value) {
		setProperty(DataQueueOutputNodeUDN.PROPERTY_KEY, value);
		return this;
	}

	public String getKey() {
		return (String)getPropertyValue(DataQueueOutputNodeUDN.PROPERTY_KEY);
	}

	public DataQueueOutputNodeUDN setKeylength(int value) {
		setProperty(DataQueueOutputNodeUDN.PROPERTY_KEYLENGTH, Integer.toString(value));
		return this;
	}

	public int getKeylength() {
		String value = (String)getPropertyValue(DataQueueOutputNodeUDN.PROPERTY_KEYLENGTH);
		return Integer.parseInt(value);
	}

	public DataQueueOutputNodeUDN setEnable(boolean value) {
		setProperty(DataQueueOutputNodeUDN.PROPERTY_ENABLE, String.valueOf(value));
		return this;
	}

	public boolean getEnable() {
		return "true".equals(getPropertyValue(DataQueueOutputNodeUDN.PROPERTY_ENABLE));
	}

	public DataQueueOutputNodeUDN setDirectory(String value) {
		setProperty(DataQueueOutputNodeUDN.PROPERTY_DIRECTORY, value);
		return this;
	}

	public String getDirectory() {
		return (String)getPropertyValue(DataQueueOutputNodeUDN.PROPERTY_DIRECTORY);
	}

	public String getNodeName() {
		String retVal = super.getNodeName();
		if (retVal == null || retVal.isEmpty()) {
			retVal = "DataQueueOutput";
		}
		return retVal;
	}
}
