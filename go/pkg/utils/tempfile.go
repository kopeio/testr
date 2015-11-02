package utils
import (
	"github.com/golang/glog"
	"os"
)

func CleanupTempFile(f *os.File) {
	err := f.Close()
	if err != nil {
		glog.Warning("error closing temp file: %v", err)
	}
	err = os.Remove(f.Name())
	if err != nil {
		glog.Warning("error deleting temp file: %v", err)
	}
}