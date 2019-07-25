package example.repository;


import example.entity.TransitionContext;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface ExampleRepository extends CrudRepository<TransitionContext, String> {
    TransitionContext findByStateMachineID(String stateMachineID);
}
