package com.tfkfan;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.spatial.SpatialStrategy;
import org.apache.lucene.spatial.prefix.RecursivePrefixTreeStrategy;
import org.apache.lucene.spatial.prefix.tree.GeohashPrefixTree;
import org.apache.lucene.spatial.prefix.tree.SpatialPrefixTree;
import org.apache.lucene.spatial.query.SpatialArgs;
import org.apache.lucene.spatial.query.SpatialOperation;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.locationtech.spatial4j.context.SpatialContext;
import org.locationtech.spatial4j.context.jts.JtsSpatialContext;
import org.locationtech.spatial4j.shape.Shape;
import org.locationtech.spatial4j.shape.ShapeFactory;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        final SpatialContext ctx = JtsSpatialContext.GEO;
        final ShapeFactory shapeFactory = ctx.getShapeFactory();

        Shape polygon = shapeFactory.polygon()
                .pointXY(0, 0)
                .pointXY(25.0, 0)
                .pointXY(25.0, 25.0)
                .pointXY(0, 25.0)
                .pointXY(0, 0)
                .build();

        int maxLevels = 11;
        SpatialPrefixTree grid = new GeohashPrefixTree(ctx, maxLevels);
        SpatialStrategy strategy = new RecursivePrefixTreeStrategy(grid, "location");


        Document doc = new Document();
        Field[] fields = strategy.createIndexableFields(polygon);
        for (Field field : fields)
            doc.add(field);
        doc.add(new StoredField("id", 8989));


        final Directory directory = new ByteBuffersDirectory();
        IndexWriterConfig iwConfig = new IndexWriterConfig();
        IndexWriter indexWriter = new IndexWriter(directory, iwConfig);
        indexWriter.addDocument(doc);
        indexWriter.forceMerge(1);
        indexWriter.close();


        final IndexReader indexReader = DirectoryReader.open(directory);
        IndexSearcher indexSearcher = new IndexSearcher(indexReader);


        Query spatialQuery = strategy.makeQuery(
                new SpatialArgs(SpatialOperation.Contains, shapeFactory.pointXY(25.0, 0))
        );

        // Perform search
        TopDocs results = indexSearcher.search(spatialQuery, 10);
        for (ScoreDoc scoreDoc : results.scoreDocs)
            System.out.println(indexSearcher.storedFields().document(scoreDoc.doc).getField("id").stringValue());
    }
}