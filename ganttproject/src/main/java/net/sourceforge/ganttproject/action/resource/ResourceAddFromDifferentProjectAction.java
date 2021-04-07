/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject Team

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.action.resource;

import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.GanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.parser.GPParser;
import net.sourceforge.ganttproject.parser.ParserFactory;
import net.sourceforge.ganttproject.parser.ResourceTagHandler;
import net.sourceforge.ganttproject.parser.RoleTagHandler;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.roles.RoleManager;
import net.sourceforge.ganttproject.task.TaskManager;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Action connected to the menu item for insert a new resource
 */
public class ResourceAddFromDifferentProjectAction extends ResourceAction {
  private final UIFacade myUIFacade;

  private final RoleManager myRoleManager;
  private final TaskManager myTaskManager;
  private final CustomPropertyManager myCustomPropertyManager;
  private final ParserFactory myParserFactory;
  private final boolean myIsProjectShareable;

  public ResourceAddFromDifferentProjectAction(HumanResourceManager hrManager, RoleManager roleManager, TaskManager taskManager, CustomPropertyManager customPropertyManager, UIFacade uiFacade, ParserFactory parserFactory, boolean isShareable) {
    super("resource.importresourcefromproject", hrManager);
    myUIFacade = uiFacade;
    myRoleManager = roleManager;
    myTaskManager = taskManager;
    myCustomPropertyManager = customPropertyManager;
    myParserFactory = parserFactory;
    myIsProjectShareable = isShareable;
  }

  ResourceAddFromDifferentProjectAction(HumanResourceManager hrManager, RoleManager roleManager, TaskManager taskManager, CustomPropertyManager customPropertyManager, UIFacade uiFacade, IconSize size, ParserFactory parserFactory, boolean isShareable) {
    super("resource.importresourcefromproject", hrManager, null, size);
    myUIFacade = uiFacade;
    myRoleManager = roleManager;
    myTaskManager = taskManager;
    myCustomPropertyManager = customPropertyManager;
    myParserFactory = parserFactory;
    myIsProjectShareable = isShareable;
  }

  @Override
  public GPAction withIcon(IconSize size) {
    return new ResourceAddFromDifferentProjectAction(getManager(), myRoleManager, myTaskManager, myCustomPropertyManager, myUIFacade, size, myParserFactory, myIsProjectShareable);
  }

  @Override
  public void actionPerformed(ActionEvent event) {
    if (calledFromAppleScreenMenu(event)) {
      return;
    }

    JFileChooser fileChooser = new JFileChooser();
    fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
    fileChooser.setFileFilter(new FileNameExtensionFilter("*.gan", "gan"));
    int result = fileChooser.showOpenDialog(myUIFacade.getMainFrame());

    if (result == JFileChooser.APPROVE_OPTION) {
      File selectedFile = fileChooser.getSelectedFile();
      System.out.println("Selected file: " + selectedFile.getAbsolutePath());
      try {
        parse(selectedFile.getAbsolutePath());
      } catch (ParserConfigurationException e) {
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      } catch (SAXException e) {
        e.printStackTrace();
      } catch (net.sourceforge.ganttproject.document.Document.DocumentException e) {
        e.printStackTrace();
      } catch (XPathExpressionException e) {
        e.printStackTrace();
      }
    }
  }

  private void parse(String filename) throws ParserConfigurationException, IOException, SAXException, net.sourceforge.ganttproject.document.Document.DocumentException, XPathExpressionException {
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    factory.setValidating(true);
    factory.setIgnoringElementContentWhitespace(true);
    DocumentBuilder builder = factory.newDocumentBuilder();
    File file = new File(filename);
    Document doc = builder.parse(file);

    // Get root node and get attributes from it
    Element project = doc.getDocumentElement();

    if (project != null && Boolean.parseBoolean(project.getAttribute("shareable"))) {
      // Project is shareable
      GPParser opener = myParserFactory.newParser();

      ResourceTagHandler resourceHandler = new ResourceTagHandler(getManager(), myRoleManager,
        myCustomPropertyManager);

      RoleTagHandler rolesHandler = new RoleTagHandler(myRoleManager);

      opener.addTagHandler(resourceHandler);
      opener.addTagHandler(rolesHandler);

      InputStream is;
      try {
        is = new FileInputStream(filename);
      } catch (IOException e) {
        throw new net.sourceforge.ganttproject.document.Document.DocumentException(GanttLanguage.getInstance().getText("msg8") + ": " + e.getLocalizedMessage(), e);
      }

      opener.load(is);
    } else {
      JOptionPane.showMessageDialog(null,
        String.format("Project %s is not shareable", filename),
        "Project is not shareable",
        JOptionPane.ERROR_MESSAGE);
    }
  }

  @Override
  public void updateAction() {
    super.updateAction();
  }

  @Override
  public ResourceAddFromDifferentProjectAction asToolbarAction() {
    ResourceAddFromDifferentProjectAction result = new ResourceAddFromDifferentProjectAction(getManager(), myRoleManager, myTaskManager, myCustomPropertyManager, myUIFacade, myParserFactory, myIsProjectShareable);
    result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result));
    return result;
  }
}
