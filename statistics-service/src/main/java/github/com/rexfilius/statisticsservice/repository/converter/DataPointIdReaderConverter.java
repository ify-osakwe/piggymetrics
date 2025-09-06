package github.com.rexfilius.statisticsservice.repository.converter;

import github.com.rexfilius.statisticsservice.model.timeseries.DataPointId;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
@ReadingConverter
public class DataPointIdReaderConverter implements Converter<Document, DataPointId> {

	@Override
	public DataPointId convert(Document document) {
		Date date = document.getDate("date");
		String account = document.getString("account");
		return new DataPointId(account, date);
	}
}


/*
 @Component
public class DataPointIdReaderConverter implements Converter<DBObject, DataPointId> {

	@Override
	public DataPointId convert(DBObject object) {

		Date date = (Date) object.get("date");
		String account = (String) object.get("account");

		return new DataPointId(account, date);
	}
}
 */
