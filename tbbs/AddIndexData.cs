using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using Tridion.ContentManager.Templating;
using Tridion.ContentManager.Templating.Assembly;

namespace SI4T.Templating
{
    /// <summary>
    /// This TBB takes search indexing data already present in the package
    /// and adds it to the rendered Output. This TBB should be put at the end of
    /// the template, after all TBBs which create/work with the Output are completed
    /// </summary>
    [TcmTemplateTitle("Add Index Data To Output")]
    public class AddIndexData : TemplateBase
    {
        public override void Transform(Engine engine, Package package)
        {
            this.Initialize(engine, package);
            string searchData = m_Package.GetValue(Constants.PACKAGE_ITEM_SEARCHDATA);
            Logger.Debug("Found Search Index Data in package: " + searchData);
            if (!String.IsNullOrEmpty(searchData))
            {
                XmlDocument outputXml = this.GetXmlDocumentFromPackage("Output");
                if (outputXml != null)
                {
                    XmlComment data = outputXml.CreateComment(String.Format(Constants.DELIMITER_PATTERN_SEARCHDATA, searchData));
                    outputXml.DocumentElement.InsertBefore(data, outputXml.DocumentElement.FirstChild);
                    this.PushXmlDocumentToPackage("Output", outputXml);
                }
                else
                {
                    String output = package.GetByName("Output").GetAsString();
                    output = String.Format("<!--{0}-->{1}", String.Format(Constants.DELIMITER_PATTERN_SEARCHDATA, searchData),output);
                    this.PushStringToPackage("Output", output);
                }
            }
            else
            {
                Logger.Debug(String.Format("No Search Index Data (package item : {0}) found - nothing to do", Constants.PACKAGE_ITEM_SEARCHDATA));
            }
        }
    }
}