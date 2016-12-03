
namespace SI4T.Query.CloudSearch.Models
{
    public class Bucket
    {
        public string Name { get; private set; }
        public long Count { get; private set; }

        public Bucket(string name, long count)
        {
            Name = name;
            Count = count;
        }
    }
}
