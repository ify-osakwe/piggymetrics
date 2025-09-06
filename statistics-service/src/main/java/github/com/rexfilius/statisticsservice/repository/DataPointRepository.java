package github.com.rexfilius.statisticsservice.repository;

import github.com.rexfilius.statisticsservice.domain.timeseries.DataPoint;
import github.com.rexfilius.statisticsservice.domain.timeseries.DataPointId;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DataPointRepository extends CrudRepository<DataPoint, DataPointId> {

	List<DataPoint> findByIdAccount(String account);

}
