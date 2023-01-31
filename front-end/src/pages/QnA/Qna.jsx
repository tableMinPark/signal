import React, { useState, useEffect } from 'react'
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow } from '@mui/material'
import Paging from 'components/Paging'
import { Link } from 'react-router-dom'
import 'assets/styles/qna.css'

function Qna() {
  const [data, setData] = useState([])

  const [size] = useState(8)
  const [page, setPage] = useState(1)
  const [count, setCount] = useState(0)

  const handlePageChange = (page) => {
    setPage(page)
    console.log(page)
  }
  useEffect(() => {
    fetch(process.env.REACT_APP_API_URL + `/board/qna?page=${page}&size=${size}`, {
      method: 'GET',
    })
      .then((res) => res.json())
      .then((res) => {
        console.log(res.body.qnaList)
        setData(res.body.qnaList)
      })
    fetch(process.env.REACT_APP_API_URL + '/board/qna/count', {
      method: 'GET',
    })
      .then((res) => res.json())
      .then((res) => {
        setCount(res.body.count)
      })
  }, [page])

  const rows = []
  Array.from(data).forEach((item) => {
    rows.push({
      id: item.qnaSeq,
      title: item.title,
      regDt: item.regDt.split(' ', 1),
      view: item.view,
    })
  })

  return (
    <div className="qna-page-container">
      <div className="qna-container">
        <div className="qna-header">Q & A</div>
        <div className="qna-table">
          <TableContainer>
            <Table>
              <TableHead className="qna-table-head">
                <TableRow>
                  <TableCell align="center">번호</TableCell>
                  <TableCell align="center">제목</TableCell>
                  <TableCell align="center">날짜</TableCell>
                  <TableCell align="center">조회수</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {rows.map((row) => (
                  <TableRow key={row.id}>
                    <TableCell align="center">{row.id}</TableCell>
                    <TableCell align="left">
                      <Link to={`/qnaDetail`} state={{ id: row.id }}>
                        {row.title}
                      </Link>
                    </TableCell>
                    <TableCell align="center">{row.regDt}</TableCell>
                    <TableCell align="center">{row.view}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          <Paging page={page} count={count} setPage={handlePageChange} />
        </div>
      </div>
    </div>
  )
}

export default Qna