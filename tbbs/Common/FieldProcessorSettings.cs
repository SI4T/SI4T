using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace SI4T.Templating
{
    /// <summary>
    /// Generic Field Processing Settings - all methods are public virtual to allow this 
    /// to be subclassed and behaviour altered/extended with ease
    /// </summary>
    public class FieldProcessorSettings
    {
        public bool? ExcludeByDefault { get; set; }
        public List<string> ManagedFields { get; set; }
        public List<string> LinkFieldsToEmbed { get; set; }
        public Dictionary<string, IndexField> FieldMap { get; set; }

        /// <summary>
        /// Set the list of managed fields (either to be included or excluded based on ExcludeByDefault property)
        /// </summary>
        /// <param name="managedFields">Comma delimited list of XML field names</param>
        public virtual void SetManagedFields(string managedFields)
        {
            if (managedFields != null)
            {
                ManagedFields = new List<string>();
                foreach (string token in managedFields.Split(','))
                {
                    string val = token.Trim();
                    if (!ManagedFields.Contains(val))
                    {
                        ManagedFields.Add(val);
                    }
                }
            }
        }
        /// <summary>
        /// Set the field map from a configuration string of the form indexFieldA:tridionfield1,tridionfield2|indexFieldB:tridionfield3 etc.
        /// A plus can be added to the index field name to indicate it allows multivalues (eg indexFieldA+:tridionfield1,tridionfield2)
        /// </summary>
        /// <param name="customFields"></param>
        public virtual void SetFieldMap(string customFields)
        {
            if (customFields != null)
            {
                FieldMap = new Dictionary<string, IndexField>();
                foreach (string token in customFields.Split('|'))
                {
                    string[] items = token.Split(':');
                    string target = items[0];
                    bool multi = false;
                    if (target.EndsWith("+"))
                    {
                        multi = true;
                        target = target.Substring(0, target.Length - 1);
                    }
                    if (items.Length > 1)
                    {
                        foreach (string source in items[1].Split(','))
                        {
                            if (!FieldMap.ContainsKey(source))
                            {
                                FieldMap.Add(source, new IndexField{Name=target,IsMultiValue=multi});
                            }
                        }
                    }
                    else if (!FieldMap.ContainsKey(target))
                    {
                        FieldMap.Add(target, new IndexField {Name = target});
                    }
                }
            }
        }

        /// <summary>
        /// Set the list of link fields to embed index data from
        /// </summary>
        /// <param name="linkfields">Comma delimited list of XML field names</param>
        public virtual void SetLinkFieldsToEmbedFields(string linkfields)
        {
            if (linkfields != null)
            {
                LinkFieldsToEmbed = new List<string>();
                foreach (string token in linkfields.Split(','))
                {
                    string val = token.Trim();
                    if (!LinkFieldsToEmbed.Contains(val))
                    {
                        LinkFieldsToEmbed.Add(val);
                    } 
                }
            }
        }

        /// <summary>
        /// Check if a link field should be followed to embed index data from linked components
        /// </summary>
        /// <param name="fieldname">Xml name of link field</param>
        /// <returns>true if data from linked component should be embedded</returns>
        public virtual bool IsLinkToBeFollowed(string fieldname)
        {
            return LinkFieldsToEmbed==null ? false : LinkFieldsToEmbed.Contains(fieldname);
        }

        /// <summary>
        /// Check if there is a mapping to a custom index field for a given field
        /// </summary>
        /// <param name="fieldname">Xml name of field</param>
        /// <returns></returns>
        public virtual bool HasCustomMapping(string fieldname)
        {
            return FieldMap==null ? false : FieldMap.ContainsKey(fieldname);
        }

        /// <summary>
        /// Get index field configuration for a custom field
        /// </summary>
        /// <param name="fieldname">Xml name of field</param>
        /// <returns>Index field configuration</returns>
        public virtual IndexField CustomFieldTarget(string fieldname)
        {
            return (FieldMap!=null && FieldMap.ContainsKey(fieldname)) ? FieldMap[fieldname] : null;
        }
    }
}
