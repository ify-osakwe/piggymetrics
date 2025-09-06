package github.com.rexfilius.statisticsservice.repository.converter;

import github.com.rexfilius.statisticsservice.model.timeseries.DataPointId;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.stereotype.Component;

@Component
@WritingConverter
public class DataPointIdWriterConverter implements Converter<DataPointId, Document> {

	@Override
	public Document convert(DataPointId id) {
		return new Document()
				.append("date", id.getDate())
				.append("account", id.getAccount());
	}
}

/*

@Component
public class DataPointIdWriterConverter implements Converter<DataPointId, DBObject> {

	private static final int FIELDS = 2;

	@Override
	public DBObject convert(DataPointId id) {

		DBObject object = new BasicDBObject(FIELDS);

		object.put("date", id.getDate());
		object.put("account", id.getAccount());

		return object;
	}
}

 */
