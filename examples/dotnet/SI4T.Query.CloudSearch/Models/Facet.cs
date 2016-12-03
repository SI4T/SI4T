using System.Collections.Generic;

namespace SI4T.Query.CloudSearch.Models
{
    public class Facet
    {
        public string Name { get; set; } 
        public List<Bucket> Buckets { get; set; }
    }
}
