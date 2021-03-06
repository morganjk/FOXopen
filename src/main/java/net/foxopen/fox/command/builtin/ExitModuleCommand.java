package net.foxopen.fox.command.builtin;


import net.foxopen.fox.XFUtil;
import net.foxopen.fox.command.Command;
import net.foxopen.fox.command.CommandFactory;
import net.foxopen.fox.command.flow.XDoControlFlow;
import net.foxopen.fox.command.flow.XDoControlFlowCST;
import net.foxopen.fox.dom.DOM;
import net.foxopen.fox.ex.ExActionFailed;
import net.foxopen.fox.ex.ExDoSyntax;
import net.foxopen.fox.ex.ExInternal;
import net.foxopen.fox.module.Mod;
import net.foxopen.fox.thread.ActionRequestContext;
import net.foxopen.fox.thread.stack.transform.CallStackTransformation;

import java.util.Collection;
import java.util.Collections;

public class ExitModuleCommand
extends BuiltInCommand {

  private final CallStackTransformation.Type mOperationType;
  private final String mUrl;

  /**
  * Constructs an Exit Module command from the XML element specified.
  *
  * @param pCommandElement the element from which the command will
  *        be constructed.
  */
  private ExitModuleCommand(DOM pCommandElement) {
    super(pCommandElement);

    String lTypeAttr = pCommandElement.getAttrOrNull("type");
    if(XFUtil.isNull(lTypeAttr)) {
      mOperationType = CallStackTransformation.Type.EXIT_THIS_PRESERVE_CALLBACKS;
    }
    else {
      mOperationType =  CallStackTransformation.Type.forName(lTypeAttr);
    }

    mUrl = pCommandElement.getAttrOrNull("uri");

    // Validate operation is an exit type
    if (mOperationType != CallStackTransformation.Type.EXIT_ALL_CANCEL_CALLBACKS &&
        mOperationType != CallStackTransformation.Type.EXIT_THIS_CANCEL_CALLBACKS &&
        mOperationType != CallStackTransformation.Type.EXIT_THIS_PRESERVE_CALLBACKS
    ) {
      throw new ExInternal("fm:exit-module: Exit module type not known: "+mOperationType.toString());
    }
  }

  @Override
  public XDoControlFlow run(ActionRequestContext pRequestContext) {
    String lExitFoxUrl = null;
    try {
      lExitFoxUrl = mUrl==null ? null : pRequestContext.getContextUElem().extendedStringOrXPathString(pRequestContext.getContextUElem().attachDOM(), mUrl);
    }
    catch (ExActionFailed e) {
      throw new ExInternal("Bad XPath for exit-module URL", e);
    }

    CallStackTransformation lCST = CallStackTransformation.createExitCallStackTransformation(mOperationType, lExitFoxUrl);

    return new XDoControlFlowCST(lCST);
  }

  public boolean isCallTransition() {
   return true;
  }

  public static class Factory
  implements CommandFactory {

    @Override
    public Command create(Mod pModule, DOM pMarkupDOM) throws ExDoSyntax {
      return new ExitModuleCommand(pMarkupDOM);
    }

    @Override
    public Collection<String> getCommandElementNames() {
      return Collections.singleton("exit-module");
    }
  }
}
