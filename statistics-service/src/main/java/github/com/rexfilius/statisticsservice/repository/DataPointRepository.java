package github.com.rexfilius.statisticsservice.repository;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import github.com.rexfilius.statisticsservice.model.timeseries.DataPoint;
import github.com.rexfilius.statisticsservice.model.timeseries.DataPointId;

import java.util.List;

@Repository
public interface DataPointRepository extends CrudRepository<DataPoint, DataPointId> {

	List<DataPoint> findByIdAccount(String account);

}
