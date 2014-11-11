using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using Tridion.ContentManager.CommunicationManagement;
using Tridion.ContentManager.ContentManagement;
using Tridion.ContentManager.Publishing.Rendering;
using Tridion.ContentManager.Templating;
using Tridion.ContentManager.Templating.Assembly;

namespace SI4T.Templating
{
    /// <summary>
    /// This TBB takes search indexing data already present in the package
    /// and adds it to the rendered Output and generates binary variants for 
    /// index data related to binaries. This TBB should be put at the end of
    /// the template, after all TBBs which create/work with the Output are completed
    /// </summary>
    [TcmTemplateTitle("Add Index Data To Output")]
    public class AddIndexData : TemplateBase
    {
        public override void Transform(Engine engine, Package package)
        {
            this.Initialize(engine, package);
            string searchData = m_Package.GetValue(Constants.PACKAGE_ITEM_SEARCHDATA);
            if (!String.IsNullOrEmpty(searchData))
            {
                Logger.Debug("Found Search Index Data in package: " + searchData);
                AddSearchDataToOutput(searchData);
            }
            else
            {
                Logger.Debug(String.Format("No Search Index Data (package item : {0}) found", Constants.PACKAGE_ITEM_SEARCHDATA));
            }
            foreach (var item in GetBinaryDataFromPackage())
            {
                CreateBinaryVariantForBinaryData(item);
            }
        }

        private List<Item> GetBinaryDataFromPackage()
        {
            var results = new List<Item>();
            foreach (var item in m_Package.GetAllByType(new ContentType("*/*")))
            {
                if (item.Properties.ContainsKey(Constants.PACKAGE_ITEM_PROPERTY_TCMURI))
                {
                    results.Add(item);
                }
            }
            return results;
        }

        private void CreateBinaryVariantForBinaryData(Item item)
        {
            var uri = item.Properties[Constants.PACKAGE_ITEM_PROPERTY_TCMURI];
            var sgUri = item.Properties[Constants.PACKAGE_ITEM_PROPERTY_SG];
            var filename = item.Properties[Constants.PACKAGE_ITEM_PROPERTY_FILENAME];
            var comp = (Component)m_Engine.GetObject(uri);
            string variant = "searchData";
            string mimetype = "text/xml";
            Binary binary = null;
            if (sgUri != null)
            {
                var sg = (StructureGroup)m_Engine.GetObject(sgUri);
                binary = m_Engine.PublishingContext.RenderedItem.AddBinary(item.GetAsStream(), filename + ".indexdata", sg, variant, comp, mimetype);
            }
            else
            {
                binary = m_Engine.PublishingContext.RenderedItem.AddBinary(item.GetAsStream(), filename + ".indexdata", variant, comp, mimetype);
            }
            Logger.Debug("Added binary index data variant: " + binary.Url);
        }

        private void AddSearchDataToOutput(string searchData)
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
                String output = m_Package.GetByName("Output").GetAsString();
                output = String.Format("<!--{0}-->{1}", String.Format(Constants.DELIMITER_PATTERN_SEARCHDATA, searchData), output);
                this.PushStringToPackage("Output", output);
            }
        }
    }
}