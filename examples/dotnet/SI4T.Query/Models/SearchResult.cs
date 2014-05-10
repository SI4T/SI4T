using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace SI4T.Query.Models
{
    public class SearchResult
    {
        public string Id { get; set; }
        public int PublicationId { get; set; }
        public string Url { get; set; }
        public string Title { get; set; }
        public string Summary { get; set; }
        public Dictionary<string, object> CustomFields { get; set; }
        public SearchResult()
        {
            CustomFields = new Dictionary<string, object>();
        }
    }
}
