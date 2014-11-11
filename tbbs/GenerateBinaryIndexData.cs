using System;
using System.Collections.Generic;
using System.Xml;
using Tridion.ContentManager.CommunicationManagement;
using Tridion.ContentManager.ContentManagement;
using Tridion.ContentManager.Templating;
using Tridion.ContentManager.Templating.Assembly;
using System.Linq;
using System.IO;

namespace SI4T.Templating
{
    /// <summary>
    /// This TBB can be used to generate basic XML items in the package
    /// containing additional data to be indexed by a search engine 
    /// related to binaries added to the package when rendering a page
    /// or component presentation
    /// </summary>
	[TcmTemplateTitle("Generate Binary Index Data")]
	public class GenerateBinaryIndexData : TemplateBase
	{
		public override void Transform(Engine engine, Package package)
		{
            this.Initialize(engine, package);
            foreach(Item binary in GetBinariesFromPackage())
            {
                FieldProcessor processor = new FieldProcessor();
                processor.Initialize(package);
                SearchData data = new SearchData(processor);
                var id = binary.Properties[Item.ItemPropertyTcmUri];
                data.ProcessComponent((Component)engine.GetObject(id), null);
                SerializeAndPushToPackage(data, binary);
            }
		}

        private List<Item> GetBinariesFromPackage()
        {
            var extensions = m_Package.GetValue(Constants.FIELD_INDEXED_BINARY_EXTENSIONS);
            Logger.Debug("Looking for binaries in package with the following extensions: " + extensions);
            var binaryExtensions = (extensions ?? "").Split(',').Select(s => s.Trim()).ToList();
            List<Item> results = new List<Item>();
            foreach (Item item in m_Package.GetAllByType(new ContentType("*/*")))
            {
                if (item.Properties.ContainsKey(Item.ItemPropertyTcmUri) && item.Properties.ContainsKey(Item.ItemPropertyFileName))
                {
                    var extension = Path.GetExtension(item.Properties[Item.ItemPropertyFileName]).Substring(1);
                    if (binaryExtensions.Contains(extension))
                    {
                        results.Add(item);
                    }
                }
            }
            return results;
        }

	}
}
