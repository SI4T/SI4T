using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;
using System.Xml.Serialization;
using Tridion.ContentManager.CommunicationManagement;
using Tridion.ContentManager.ContentManagement;
using Tridion.ContentManager.ContentManagement.Fields;

namespace SI4T.Templating
{
    /// <summary>
    /// Generic search indexing data class - all methods are public virtual to allow this to
    /// be subclassed and extended/altered with ease
    /// </summary>
    [XmlRoot("indexdata")]
    public class SearchData 
    {
        //Url for the item, not this can also be a uri or other identifier used to load a resource
        [XmlElement("url")]
        public string Url { get; set; }
        
        //Title to show when results displayed
        [XmlElement("title")]
        public string Title { get; set; }
        
        [XmlElement("publicationid")]
        public int PublicationId { get; set; }
        
        [XmlElement("schemaid")]
        public int SchemaId { get; set; }

        //16 for component, 64 for page
        [XmlElement("itemtype")]
        public int ItemType { get; set; }
        
        [XmlElement("parentsgid")]
        public int ParentSGId { get; set; }
        
        //All the parent, grandparent etc. SGs
        [XmlElement("sgid")]
        public List<int> StructureGroups { get; set; }
        
        //Used to enable searches to be restricted to certain areas, and to exclude content from general search
        [XmlElement("type")]
        public int Type { get; set; }

        //Used to process content and metadata field sets and build up index data
        private FieldProcessor _processor;
        
        //flag indicating that the item processed is to be indexed
        private bool _hasIndexData = false;
        
        public SearchData(FieldProcessor processor)
            : this()
        {
            _processor = processor;
        }

        public SearchData()
        {
            _processor = new FieldProcessor();
        }

        /// <summary>
        /// Prepare index data for a Page
        /// </summary>
        /// <param name="page">The page to process</param>
        /// <returns>List of processed DCPs - can be used to prevent DCPs being indexed multiple times (both in page and CP rendering)</returns>
        public virtual List<string> ProcessPage(Page page)
        {
            List<string> processedDcps = new List<string>();
            if (page.IsIndexed() && page.PageTemplate.IsIndexed() && page.OrganizationalItem.IsIndexed())
            {
                this.Url = page.PublishLocationUrl;
                this.SchemaId = page.ComponentPresentations.Count > 0 ? page.ComponentPresentations[0].Component.Schema.Id.ItemId: 0;
                this.PublicationId = page.ContextRepository.Id.ItemId;
                this.ItemType = 64;
                this.ParentSGId = page.OrganizationalItem.Id.ItemId;
                StructureGroups = new List<int>();
                var sg = page.OrganizationalItem;
                while (sg!=null && sg is StructureGroup)
                {
                    StructureGroups.Add(sg.Id.ItemId);
                    sg = sg.OrganizationalItem;
                }
                ProcessPageMetadata(page);
                foreach (var cp in page.ComponentPresentations)
                {
                    if (cp.ComponentTemplate.IsIndexed(_processor.MinimumComponentTemplatePrio))
                    {
                        FieldProcessorSettings settings = cp.ComponentTemplate.GetFieldProcessorSettings();
                        ProcessComponentData(cp.Component, settings);
                        _hasIndexData = true;
                        //To avoid DCPs embedded on pages being indexed twice in the same publish transaction
                        //(once in the page index data and again in their own index data)
                        //we return identifiers to the template, to add to context variables
                        if (cp.ComponentTemplate.IsRepositoryPublishable)
                        {
                            processedDcps.Add(GetDcpIdentifier(cp));
                        }
                    }
                }
                if (String.IsNullOrEmpty(_processor.Title))
                {
                    this.Title = page.Title;
                }
                else
                {
                    this.Title = _processor.Title;
                }
            }
            return processedDcps;
        }

        /// <summary>
        /// Prepare index data for a component presentation
        /// </summary>
        /// <param name="cp">Component Presentation to process</param>
        /// <param name="flaggedDcps">List of already processed CPs, to avoid processing the same DCP more than once</param>
        public virtual void ProcessComponentPresentation(ComponentPresentation cp, List<string> flaggedDcps)
        {
            string id = GetDcpIdentifier(cp);
            if (cp.ComponentTemplate.IsIndexed(_processor.MinimumComponentTemplatePrio) && (flaggedDcps == null || !flaggedDcps.Contains(id)))
            {
                this.Url = GetUrlForDcp(cp);
                FieldProcessorSettings settings = cp.ComponentTemplate.GetFieldProcessorSettings();
                ProcessComponent(cp.Component, settings);
            }
        }

        /// <summary>
        /// Prepare index data for a component, as part of a dynamic component presentation
        /// </summary>
        /// <param name="comp">The component to process</param>
        /// <param name="settings">field processor settings</param>
        public virtual void ProcessComponent(Component comp, FieldProcessorSettings settings)
        {
            if (comp.IsIndexed() && comp.OrganizationalItem.IsIndexed())
            {
                this.PublicationId = comp.ContextRepository.Id.ItemId;
                this.ItemType = 16;
                this.SchemaId = comp.Schema.Id.ItemId;
                ProcessComponentData(comp, settings);
                _hasIndexData = true;
                if (String.IsNullOrEmpty(_processor.Title))
                {
                    this.Title = comp.Title;
                }
                else
                {
                    this.Title = _processor.Title;
                }
            }
        }

        /// <summary>
        /// Prepare index data for page metadata
        /// </summary>
        /// <param name="page">The page to process</param>
        public virtual void ProcessPageMetadata(Page page)
        {
            FieldProcessorSettings settings = page.PageTemplate.GetFieldProcessorSettings();
            ProcessMetadata(page, settings);
        }

        /// <summary>
        /// Prepare index data for any item metadata
        /// </summary>
        /// <param name="item">The item to process</param>
        /// <param name="settings">Field processor settings</param>
        public virtual void ProcessMetadata(RepositoryLocalObject item, FieldProcessorSettings settings)
        {
            if (item.Metadata != null)
            {
                ItemFields fields = new ItemFields(item.Metadata, item.MetadataSchema);
                _processor.ProcessData(fields, settings);
            }
        }

        /// <summary>
        /// Prepare index data for a component
        /// </summary>
        /// <param name="comp">The component to process</param>
        /// <param name="settings">field processor settings</param>
        public virtual void ProcessComponentData(Component component, FieldProcessorSettings settings)
        {
            _processor.SetComponentAsProcessed(component.Id);
            if (component.IsIndexed())
            {
                if (component.Content != null)
                {
                    ItemFields fields = new ItemFields(component.Content, component.Schema);
                    _processor.ProcessData(fields, settings);
                }
                ProcessMetadata(component, settings);
            }
        }

        /// <summary>
        /// Set a custom index field value
        /// </summary>
        /// <param name="fieldName">custom field name</param>
        /// <param name="value">value for the index field</param>
        public virtual void SetCustomField(string fieldName, object value)
        {
            SetCustomFields(fieldName, new List<object> { value });
        }

        /// <summary>
        /// Set custom index field values
        /// </summary>
        /// <param name="fieldName">custom field name</param>
        /// <param name="values">values for the index field</param>
        public virtual void SetCustomFields(string fieldName, List<object> values)
        {
            _processor.SetCustomFieldValues(fieldName, values);
        }

        /// <summary>
        /// Get the catchall default index data element
        /// </summary>
        /// <returns>the catchall element</returns>
        public virtual XmlElement GetCatchAllElement()
        {
            return _processor.GetCatchAllElement();
        }

        /// <summary>
        /// Get a custom index data element
        /// </summary>
        /// <returns>the custom element</returns>
        public virtual XmlElement GetCustomElement()
        {
            return _processor.GetCustomElement();
        }

        /// <summary>
        /// Check if any data has been prepared for indexing
        /// </summary>
        /// <returns></returns>
        public virtual bool HasIndexData()
        {
            return _hasIndexData;
        }

        /// <summary>
        /// Get an ID that will uniquely identify a DCP
        /// </summary>
        /// <param name="cp">The DCP</param>
        /// <returns>Id in the form: dcp:{pubid}-{compid}-{ctid}</returns>
        public virtual string GetDcpIdentifier(ComponentPresentation cp)
        {
            return GetDcpIdentifier(cp.Component.Id.PublicationId, cp.Component.Id.ItemId, cp.ComponentTemplate.Id.ItemId);
        }

        /// <summary>
        /// Get an ID that will uniquely identify a DCP
        /// </summary>
        /// <param name="publicationId"></param>
        /// <param name="componentId"></param>
        /// <param name="templateId"></param>
        /// <returns>Id in the form: dcp:{pubid}-{compid}-{ctid}</returns>
        public virtual string GetDcpIdentifier(int publicationId, int componentId, int templateId)
        {
            return String.Format("dcp:{0}-{1}-{2}", publicationId, componentId, templateId);
        }

        public virtual string GetUrlForDcp(ComponentPresentation cp)
        {
            //Need to cover the case where a DCP is allowed on page - should we then use dynamic linking to get the URL?
            return cp.Component.Id.ToString();
        }
    }
}
