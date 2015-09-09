using System;
using System.Collections.Generic;
using System.Xml;
using Tridion.ContentManager.CommunicationManagement;
using Tridion.ContentManager.ContentManagement;
using Tridion.ContentManager.Templating;
using Tridion.ContentManager.Templating.Assembly;

namespace SI4T.Templating
{
    /// <summary>
    /// This TBB can be used to generate basic XML item in the package
    /// containing page or component data to be indexed by a search
    /// engine when publishing a Page or Dynamic Component Presentation
    /// </summary>
	[TcmTemplateTitle("Generate Index Data")]
    [TcmTemplateParameterSchema("resource:SI4T.Templating.xsd.Search Indexing TBB Parameters.xsd")]
	public class GenerateIndexData : TemplateBase
	{
		public override void Transform(Engine engine, Package package)
		{
            this.Initialize(engine, package);

            if (IsTargetIndexed())
            {
                //Initialize field processor from package variables
                FieldProcessor processor = new FieldProcessor();
                processor.Initialize(package);
                SearchData data = new SearchData(processor);
                if (this.IsPageTemplate)
                {
                    UpdateFlaggedDcps(data.ProcessPage(this.GetPage()));
                }
                else
                {
                    data.ProcessComponentPresentation(new Tridion.ContentManager.CommunicationManagement.ComponentPresentation(this.GetComponent(), this.GetComponentTemplate()), GetFlaggedDcps());
                }
                SerializeAndPushToPackage(data);
            }
		}

        public virtual List<string> GetFlaggedDcps()
        {
            return m_Engine.PublishingContext.RenderContext.ContextVariables[Constants.CONTEXT_VARIABLE_FLAGGED_DCPS] as List<string>;
        }

        //We store a list of DCPs that have already been indexed as part of a page index action, in order that we can avoid indexing them again as
        //part of a DCP indexing action
        public virtual void UpdateFlaggedDcps(List<string> dcpList)
        {
            List<string> list = GetFlaggedDcps();
            if (list == null)
            {
                list = dcpList;
            }
            else
            {
                foreach (var item in dcpList)
                {
                    if (!list.Contains(item))
                    {
                        list.Add(item);
                    }
                }
            }
            m_Engine.PublishingContext.RenderContext.ContextVariables[Constants.CONTEXT_VARIABLE_FLAGGED_DCPS] = list;
        }

        protected bool IsTargetIndexed()
        {
            //For Tridion next we could check if target supports search, but for now
            //We just specifically exclude session preview only
            return !IsFastTrackPublishing();
        }
	}
}
