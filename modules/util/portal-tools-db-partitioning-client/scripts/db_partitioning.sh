#!/bin/bash

#
# Ignore SIGHUP to avoid stopping partitioning when terminal disconnects.
#

trap '' 1

if [ -e /proc/$$/fd/255 ]
then
	DB_PARTITIONING_PATH=`readlink /proc/$$/fd/255 2>/dev/null`
fi

if [ ! -n "${DB_PARTITIONING_PATH}" ]
then
	DB_PARTITIONING_PATH="$0"
fi

cd "$(dirname "${DB_PARTITIONING_PATH}")"

#
# Check running process.
#

DB_PARTITIONING_PID=db_partitioning.pid

if [ -f "${DB_PARTITIONING_PID}" ]
then
	if [ -s "${DB_PARTITIONING_PID}" ]
	then
		if [ -r "${DB_PARTITIONING_PID}" ]
		then
			PID=`cat "${DB_PARTITIONING_PID}"`

			ps -p ${PID} >/dev/null 2>&1

			if [ $? -eq 0 ]
			then
				echo "Database partitioning client is already running with process ID ${PID}."
				echo ""
				echo "If the following process is not the database partitioning client process, remove ${DB_PARTITIONING_PID} and try again."

				ps -f -p ${PID}

				exit 1
			else
				echo "Removing stale ${DB_PARTITIONING_PID}."

				rm -f "${DB_PARTITIONING_PID}" >/dev/null 2>&1

				if [ $? != 0 ]
				then
					if [ -w "${DB_PARTITIONING_PID}" ]
					then
						cat /dev/null > "${DB_PARTITIONING_PID}"
					else
						echo "Unable to remove stale ${DB_PARTITIONING_PID}."

						exit 1
					fi
				fi
			fi
		else
			echo "Unable to read ${DB_PARTITIONING_PID}."

			exit 1
		fi
	else
		rm -f "${DB_PARTITIONING_PID}" >/dev/null 2>&1

		if [ $? != 0 ]
		then
			if [ ! -w "${DB_PARTITIONING_PID}" ]
			then
				echo "Unable to write to ${DB_PARTITIONING_PID}."

				exit 1
			fi
		fi
	fi
fi

echo $$ > ${DB_PARTITIONING_PID}

#
# Run database partitioning client.
#

java -jar com.liferay.portal.tools.db.partitioning.client.jar "$@"

#
# Clean up.
#

rm ${DB_PARTITIONING_PID}