using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

namespace SI4T.Query.Models
{
    public class SearchResults
    {
        public int Total { get; set; }
        public int Start { get; set; }
        public int PageSize { get; set; }
        public List<SearchResult> Items { get; set; }
        public bool HasError { get; set; }
        public string ErrorDetail { get; set; }
        public string QueryText { get; set; }
        public SearchResults()
        {
            Items = new List<SearchResult>();
        }
    }
}
