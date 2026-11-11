package com.ibm.swg.iseries.pcml;

import java.io.Serial;
import com.ibm.broker.config.appdev.InputTerminal;
import com.ibm.broker.config.appdev.Node;
import com.ibm.broker.config.appdev.NodeProperty;
import com.ibm.broker.config.appdev.OutputTerminal;

public class ISeriesPcmlCallNodeUDN extends Node {
	@Serial
	private static final long serialVersionUID = 1L;
	protected static final String NODE_TYPE_NAME = "com/ibm/swg/iseries/pcml/ISeriesPcmlCallNode";
	protected static final String NODE_GRAPHIC_16 = "platform:/plugin/ISeries/icons/full/obj16/com/ibm/swg/iseries/pcml/ISeriesPcmlCall.png";
	protected static final String NODE_GRAPHIC_32 = "platform:/plugin/ISeries/icons/full/obj30/com/ibm/swg/iseries/pcml/ISeriesPcmlCall.png";
	protected static final String PROPERTY_CONFIGURABLESERVICE = "configurableService";
	protected static final String PROPERTY_ISXPCML = "isXPcml";
	protected static final String PROPERTY_PCMLCALL = "pcmlCall";
	protected static final String PROPERTY_LIBRARYLIST = "libraryList";
	protected static final String PROPERTY_CCSID = "ccsid";
	protected static final String PROPERTY_TOPCML = "toPcml";
	protected static final String PROPERTY_TOXPCML = "toXPcml";
	protected static final String PROPERTY_FROMXPCML = "fromXPcml";
	protected static final String PROPERTY_ENABLE = "enable";
	protected static final String PROPERTY_DIRECTORY = "directory";

	protected NodeProperty[] getNodeProperties() {
		return new NodeProperty[] {
			new NodeProperty(ISeriesPcmlCallNodeUDN.PROPERTY_CONFIGURABLESERVICE,		NodeProperty.Usage.MANDATORY,	false,	NodeProperty.Type.STRING, "myISeries","","",	"com/ibm/swg/iseries/pcml/ISeriesPcmlCall",	"ISeries"),
			new NodeProperty(ISeriesPcmlCallNodeUDN.PROPERTY_ISXPCML,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.BOOLEAN, "false","","",	"com/ibm/swg/iseries/pcml/ISeriesPcmlCall",	"ISeries"),
			new NodeProperty(ISeriesPcmlCallNodeUDN.PROPERTY_PCMLCALL,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, null,"","",	"com/ibm/swg/iseries/pcml/ISeriesPcmlCall",	"ISeries"),
			new NodeProperty(ISeriesPcmlCallNodeUDN.PROPERTY_LIBRARYLIST,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, null,"","",	"com/ibm/swg/iseries/pcml/ISeriesPcmlCall",	"ISeries"),
			new NodeProperty(ISeriesPcmlCallNodeUDN.PROPERTY_CCSID,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.INTEGER, "1208","","",	"com/ibm/swg/iseries/pcml/ISeriesPcmlCall",	"ISeries"),
			new NodeProperty(ISeriesPcmlCallNodeUDN.PROPERTY_TOPCML,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, "transformToPcml.xsl","","",	"com/ibm/swg/iseries/pcml/ISeriesPcmlCall",	"ISeries"),
			new NodeProperty(ISeriesPcmlCallNodeUDN.PROPERTY_TOXPCML,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, "transformToXPcml.xsl","","",	"com/ibm/swg/iseries/pcml/ISeriesPcmlCall",	"ISeries"),
			new NodeProperty(ISeriesPcmlCallNodeUDN.PROPERTY_FROMXPCML,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, "transformFromXPcml.xsl","","",	"com/ibm/swg/iseries/pcml/ISeriesPcmlCall",	"ISeries"),
			new NodeProperty(ISeriesPcmlCallNodeUDN.PROPERTY_ENABLE,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.BOOLEAN, "false","","",	"com/ibm/swg/iseries/pcml/ISeriesPcmlCall",	"ISeries"),
			new NodeProperty(ISeriesPcmlCallNodeUDN.PROPERTY_DIRECTORY,		NodeProperty.Usage.OPTIONAL,	false,	NodeProperty.Type.STRING, null,"","",	"com/ibm/swg/iseries/pcml/ISeriesPcmlCall",	"ISeries")
		};
	}

	public ISeriesPcmlCallNodeUDN() {
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

	public ISeriesPcmlCallNodeUDN setConfigurableService(String value) {
		setProperty(ISeriesPcmlCallNodeUDN.PROPERTY_CONFIGURABLESERVICE, value);
		return this;
	}

	public String getConfigurableService() {
		return (String)getPropertyValue(ISeriesPcmlCallNodeUDN.PROPERTY_CONFIGURABLESERVICE);
	}

	public ISeriesPcmlCallNodeUDN setIsXPcml(boolean value) {
		setProperty(ISeriesPcmlCallNodeUDN.PROPERTY_ISXPCML, String.valueOf(value));
		return this;
	}

	public boolean getIsXPcml() {
		return "true".equals(getPropertyValue(ISeriesPcmlCallNodeUDN.PROPERTY_ISXPCML));
	}

	public ISeriesPcmlCallNodeUDN setPcmlCall(String value) {
		setProperty(ISeriesPcmlCallNodeUDN.PROPERTY_PCMLCALL, value);
		return this;
	}

	public String getPcmlCall() {
		return (String)getPropertyValue(ISeriesPcmlCallNodeUDN.PROPERTY_PCMLCALL);
	}

	public ISeriesPcmlCallNodeUDN setLibraryList(String value) {
		setProperty(ISeriesPcmlCallNodeUDN.PROPERTY_LIBRARYLIST, value);
		return this;
	}

	public String getLibraryList() {
		return (String)getPropertyValue(ISeriesPcmlCallNodeUDN.PROPERTY_LIBRARYLIST);
	}

	public ISeriesPcmlCallNodeUDN setCcsid(int value) {
		setProperty(ISeriesPcmlCallNodeUDN.PROPERTY_CCSID, Integer.toString(value));
		return this;
	}

	public int getCcsid() {
		return Integer.parseInt((String)getPropertyValue(ISeriesPcmlCallNodeUDN.PROPERTY_CCSID));
	}

	public ISeriesPcmlCallNodeUDN setToPcml(String value) {
		setProperty(ISeriesPcmlCallNodeUDN.PROPERTY_TOPCML, value);
		return this;
	}

	public String getToPcml() {
		return (String)getPropertyValue(ISeriesPcmlCallNodeUDN.PROPERTY_TOPCML);
	}

	public ISeriesPcmlCallNodeUDN setToXPcml(String value) {
		setProperty(ISeriesPcmlCallNodeUDN.PROPERTY_TOXPCML, value);
		return this;
	}

	public String getToXPcml() {
		return (String)getPropertyValue(ISeriesPcmlCallNodeUDN.PROPERTY_TOXPCML);
	}

	public ISeriesPcmlCallNodeUDN setFromXPcml(String value) {
		setProperty(ISeriesPcmlCallNodeUDN.PROPERTY_FROMXPCML, value);
		return this;
	}

	public String getFromXPcml() {
		return (String)getPropertyValue(ISeriesPcmlCallNodeUDN.PROPERTY_FROMXPCML);
	}

	public ISeriesPcmlCallNodeUDN setEnable(boolean value) {
		setProperty(ISeriesPcmlCallNodeUDN.PROPERTY_ENABLE, String.valueOf(value));
		return this;
	}

	public boolean getEnable() {
		return "true".equals(getPropertyValue(ISeriesPcmlCallNodeUDN.PROPERTY_ENABLE));
	}

	public ISeriesPcmlCallNodeUDN setDirectory(String value) {
		setProperty(ISeriesPcmlCallNodeUDN.PROPERTY_DIRECTORY, value);
		return this;
	}

	public String getDirectory() {
		return (String)getPropertyValue(ISeriesPcmlCallNodeUDN.PROPERTY_DIRECTORY);
	}

	public String getNodeName() {
		String retVal = super.getNodeName();
		if (retVal == null || retVal.isEmpty()) {
			retVal = "ISeriesPcmlCall";
		}
		return retVal;
	}
}
