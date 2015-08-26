using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using Tridion.ContentManager.Templating.Assembly;
using Tridion.ContentManager.Templating;
using Tridion.ContentManager.ContentManagement;
using Tridion.ContentManager.CommunicationManagement;
using Tridion.ContentManager.Publishing.Rendering;
using Tridion.ContentManager;
using System.IO;
using System.Xml;
using Tridion.ContentManager.ContentManagement.Fields;
using System.Xml.Linq;
using System.Xml.Serialization;
using Tridion.ContentManager.Publishing;

namespace SI4T.Templating
{
    /// <summary>
    /// Base class for common functionality used by TBBs
    /// </summary>
    public abstract class TemplateBase : ITemplate
    {
        protected Engine m_Engine;
        protected Package m_Package;
        private TemplatingLogger m_Logger;
        protected int m_RenderContext = -1;
        protected TemplatingLogger Logger
        {
            get
            {
                if (m_Logger == null) m_Logger = TemplatingLogger.GetLogger(this.GetType());

                return m_Logger;
            }
        }
        /// <summary>
        /// Initializes the engine and package to use in this TemplateBase object.
        /// </summary>
        /// <param name="engine">The engine to use in calls to the other methods of this TemplateBase object</param>
        /// <param name="package">The package to use in calls to the other methods of this TemplateBase object</param>
        protected void Initialize(Engine engine, Package package)
        {
            m_Engine = engine;
            m_Package = package;
        }

        public virtual void Transform(Engine engine, Package package) { }

        /// <summary>
        /// Checks whether the TemplateBase object has been initialized correctly.
        /// This method should be called from any method that requires the <c>m_Engine</c>, 
        /// <c>m_Package</c> or <c>_log</c> member fields.
        /// </summary>
        protected void CheckInitialized()
        {
            if (m_Engine == null || m_Package == null)
            {
                throw new InvalidOperationException("This method can not be invoked, unless Initialize has been called");
            }
        }

        #region Get context objects and information

        /// <summary>
        /// True if the rendering context is a page, rather than component
        /// </summary>
        protected bool IsPageTemplate
        {
            get
            {
                if (m_RenderContext == -1)
                {
                    if (m_Engine.PublishingContext.ResolvedItem.Item is Page)
                        m_RenderContext = 1;
                    else
                        m_RenderContext = 0;
                }
                if (m_RenderContext == 1)
                    return true;
                else
                    return false;
            }
        }
        /// <summary>
        /// Returns the component object that is defined in the package for this template.
        /// </summary>
        /// <remarks>
        /// This method should only be called when there is an actual Component item in the package. 
        /// It does not currently handle the situation where no such item is available.
        /// </remarks>
        /// <returns>the component object that is defined in the package for this template.</returns>
        protected Component GetComponent()
        {
            CheckInitialized();
            Item component = m_Package.GetByName("Component");
            return (Component)m_Engine.GetObject(component.GetAsSource().GetValue("ID"));
        }

        /// <summary>
        /// Returns the Template from the resolved item if it's a Component Template
        /// </summary>
        /// <returns>A Component Template or null</returns>
        protected ComponentTemplate GetComponentTemplate()
        {
            CheckInitialized();
            Template template = m_Engine.PublishingContext.ResolvedItem.Template;

            // "if (template is ComponentTemplate)" might work instead
            if (template.GetType().Name.Equals("ComponentTemplate"))
            {
                return (ComponentTemplate)template;
            }
            else
            {
                return null;
            }
        }

        /// <summary>
        /// Returns the page object that is defined in the package for this template.
        /// </summary>
        /// <remarks>
        /// This method should only be called when there is an actual Page item in the package. 
        /// It does not currently handle the situation where no such item is available.
        /// </remarks>
        /// <returns>the page object that is defined in the package for this template.</returns>
        protected Page GetPage()
        {
            CheckInitialized();
            //first try to get from the render context
            RenderContext renderContext = m_Engine.PublishingContext.RenderContext;
            if (renderContext != null)
            {
                Page contextPage = renderContext.ContextItem as Page;
                if (contextPage != null)
                    return contextPage;
            }
            Item pageItem = m_Package.GetByType(ContentType.Page);
            if (pageItem != null)
                return (Page)m_Engine.GetObject(pageItem.GetAsSource().GetValue("ID"));

            return null;
        }

        /// <summary>
        /// Returns the publication object that can be determined from the package for this template.
        /// </summary>
        /// <remarks>
        /// This method currently depends on a Page item being available in the package, meaning that
        /// it will only work when invoked from a Page Template.
        /// 
        /// </remarks>
        /// <returns>the Publication object that can be determined from the package for this template.</returns>
        protected Publication GetPublication()
        {
            CheckInitialized();

            RepositoryLocalObject pubItem = null;
            Repository repository = null;

            if (m_Package.GetByType(ContentType.Page) != null)
                pubItem = GetPage();
            else
                pubItem = GetComponent();

            if (pubItem != null) repository = pubItem.ContextRepository;

            return repository as Publication;
        }
        #endregion

        /// <summary>
        /// Serialize an object as XML
        /// </summary>
        /// <param name="data">object to serialize</param>
        /// <returns>string containing XML serialization</returns>
        protected string SerializeObjectToXml(object data, XmlSerializerNamespaces namespaces = null)
        {
            StringBuilder sb = new StringBuilder();
            using (XmlWriter xw = XmlWriter.Create(sb, new XmlWriterSettings { OmitXmlDeclaration = true }))
            {
                SerializeObjectToXml(data, xw, namespaces);
                return sb.ToString();
            }
        }

        /// <summary>
        /// Serialize an object to XML
        /// </summary>
        /// <param name="data">object to serialize</param>
        /// <param name="writer">XmlWriter to write the serialized data to</param>
        protected void SerializeObjectToXml(object data, XmlWriter writer, XmlSerializerNamespaces namespaces=null)
        {
            if (namespaces == null)
            {
                namespaces = new XmlSerializerNamespaces();
            }
            XmlSerializer serializer = new XmlSerializer(data.GetType());
            serializer.Serialize(writer, data, namespaces);
        }

        /// <summary>
        /// Get a package item as an XmlDocument
        /// </summary>
        /// <param name="packageItemName">the package item name</param>
        /// <returns>the item as an XmlDocument</returns>
        protected XmlDocument GetXmlDocumentFromPackage(string packageItemName)
        {
            Item item = m_Package.GetByName(packageItemName);
            if (item == null)
            {
                throw new Exception(String.Format("No '{0}' package item found to create Xml Document from", packageItemName));
            }
            return item.GetAsXmlDocument();
        }

        /// <summary>
        /// Push an XmlDocument item into the package
        /// </summary>
        /// <param name="packageItemName">the package item name</param>
        /// <param name="data">the XmlDocument to push</param>
        protected void PushXmlDocumentToPackage(string packageItemname, XmlDocument data)
        {
            Item item = m_Package.GetByName(packageItemname);
            if (item != null)
            {
                m_Package.Remove(item);
            }
            m_Package.PushItem(packageItemname, m_Package.CreateXmlDocumentItem(ContentType.Xml, data));
        }

        /// <summary>
        /// Push a string into the package
        /// </summary>
        /// <param name="packageItemname">the package item name</param>
        /// <param name="data">the string to push</param>
        protected void PushStringToPackage(string packageItemname, String data)
        {
            Item item = m_Package.GetByName(packageItemname);
            if (item != null)
            {
                m_Package.Remove(item);
            }
            m_Package.PushItem(packageItemname, m_Package.CreateStringItem(ContentType.Text, data));
        }

        /// <summary>
        /// Serialize search index data and push it into the package
        /// </summary>
        /// <param name="searchData">search data to serialize</param>
        protected void SerializeAndPushToPackage(SearchData searchData)
        {
            if (searchData.HasIndexData())
            {
                //prevent unwanted namespaces from appearing in the output
                XmlSerializerNamespaces ns = new XmlSerializerNamespaces();
                ns.Add("", ""); 
                XmlDocument xmlData = new XmlDocument();
                xmlData.LoadXml(SerializeObjectToXml(searchData, ns));
                XmlElement body = searchData.GetCatchAllElement();
                XmlElement custom = searchData.GetCustomElement();
                XmlNode bodyNode = xmlData.ImportNode(body, true);
                XmlNode customNode = xmlData.ImportNode(custom, true);
                xmlData.DocumentElement.AppendChild(bodyNode);
                xmlData.DocumentElement.AppendChild(customNode);
                this.PushXmlDocumentToPackage(Constants.PACKAGE_ITEM_SEARCHDATA, xmlData);
            }
        }

        /// <summary>
        /// Determine whether we are publishing for XPM Session Preview
        /// </summary>
        /// <returns></returns>
        protected bool IsFastTrackPublishing()
        {
            return m_Engine.RenderMode == RenderMode.PreviewDynamic && m_Engine.PublishingContext.PublicationTarget != null;
        }

    }


}
