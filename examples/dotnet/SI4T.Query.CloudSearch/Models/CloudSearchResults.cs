using System.Collections.Generic;
using SI4T.Query.Models;

namespace SI4T.Query.CloudSearch.Models
{
    public class CloudSearchResults : SearchResults
    {
        public List<Facet> Facets { get; set; }

        public CloudSearchResults()
        {
            Facets = new List<Facet>();
        }
    }
}
