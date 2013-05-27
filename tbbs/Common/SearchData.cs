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

        public List<string> ProcessPage(Page page)
        {
            List<string> processedDcps = new List<string>();
            if (page.IsIndexed() && page.PageTemplate.IsIndexed())
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

        public void ProcessComponentPresentation(ComponentPresentation cp, List<string> flaggedDcps)
        {
            string id = GetDcpIdentifier(cp);
            if (cp.ComponentTemplate.IsIndexed(_processor.MinimumComponentTemplatePrio) && flaggedDcps != null && !flaggedDcps.Contains(id))
            {
                this.Url = GetUrlForDcp(cp);
                FieldProcessorSettings settings = cp.ComponentTemplate.GetFieldProcessorSettings();
                ProcessComponent(cp.Component, settings);
            }
        }

        public void ProcessComponent(Component comp, FieldProcessorSettings settings)
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

        private void ProcessPageMetadata(Page page)
        {
            FieldProcessorSettings settings = page.PageTemplate.GetFieldProcessorSettings();
            ProcessMetadata(page, settings);
        }

        private void ProcessMetadata(RepositoryLocalObject item, FieldProcessorSettings settings)
        {
            if (item.Metadata != null)
            {
                ItemFields fields = new ItemFields(item.Metadata, item.MetadataSchema);
                _processor.ProcessData(fields, settings);
            }
        }

        private void ProcessComponentData(Component component, FieldProcessorSettings settings)
        {
            ItemFields fields = new ItemFields(component.Content,component.Schema);
            _processor.ProcessData(fields, settings);
            ProcessMetadata(component, settings);
        }

        public void SetCustomField(string fieldName, object value)
        {
            SetCustomFields(fieldName, new List<object> { value });
        }

        public void SetCustomFields(string fieldName, List<object> values)
        {
            _processor.SetCustomFields(fieldName, values);
        }

        public XmlElement GetCatchAllElement()
        {
            return _processor.GetCatchAllElement();
        }

        public XmlElement GetCustomElement()
        {
            return _processor.GetCustomElement();
        }

        public bool HasIndexData()
        {
            return _hasIndexData;
        }

        public string GetDcpIdentifier(ComponentPresentation cp)
        {
            return GetDcpIdentifier(cp.Component.Id.PublicationId, cp.Component.Id.ItemId, cp.ComponentTemplate.Id.ItemId);
        }

        public string GetDcpIdentifier(int publicationId, int componentId, int templateId)
        {
            return String.Format("dcp:{0}-{1}-{2}", publicationId, componentId, templateId);
        }

        private string GetUrlForDcp(ComponentPresentation cp)
        {
            //Need to cover the case where a DCP is allowed on page - should we then use dynamic linking to get the URL?
            return cp.Component.Id.ToString();
        }
    }
}
